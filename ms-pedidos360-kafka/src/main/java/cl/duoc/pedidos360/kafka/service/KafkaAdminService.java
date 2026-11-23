package cl.duoc.pedidos360.kafka.service;

import cl.duoc.pedidos360.kafka.dto.admin.CreateTopicRequest;
import cl.duoc.pedidos360.kafka.dto.admin.ResetOffsetsRequest;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Capa de administración de Kafka sobre el AdminClient:
 * clúster, tópicos (crear, listar, describir, modificar, eliminar, aumentar particiones),
 * configuraciones y consumer groups (offsets, lag, reset y eliminación).
 */
@Service
public class KafkaAdminService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_SECONDS = 15;

    /** Solo se permite modificar estas configuraciones desde la API. */
    private static final Set<String> EDITABLE_CONFIGS = Set.of(
            TopicConfig.RETENTION_MS_CONFIG,
            TopicConfig.RETENTION_BYTES_CONFIG,
            TopicConfig.CLEANUP_POLICY_CONFIG,
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
            TopicConfig.SEGMENT_MS_CONFIG,
            TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
            TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG,
            TopicConfig.DELETE_RETENTION_MS_CONFIG);

    private static final Comparator<TopicPartition> BY_TOPIC_PARTITION =
            Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition);

    private final Admin admin;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.admin = Admin.create(kafkaAdmin.getConfigurationProperties());
    }

    @Override
    public void destroy() {
        admin.close(Duration.ofSeconds(5));
    }

    // ── Clúster ──────────────────────────────────────────────────────────────

    public Map<String, Object> clusterInfo() throws Exception {
        DescribeClusterResult cluster = admin.describeCluster();
        Node controller = await(cluster.controller());
        List<Map<String, Object>> brokers = new ArrayList<>();
        for (Node node : await(cluster.nodes())) {
            Map<String, Object> broker = new LinkedHashMap<>();
            broker.put("id", node.id());
            broker.put("host", node.host());
            broker.put("port", node.port());
            broker.put("controller", controller != null && controller.id() == node.id());
            brokers.add(broker);
        }
        brokers.sort(Comparator.comparingInt(b -> (Integer) b.get("id")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusterId", await(cluster.clusterId()));
        result.put("controllerId", controller != null ? controller.id() : null);
        result.put("brokers", brokers);
        return result;
    }

    // ── Tópicos ──────────────────────────────────────────────────────────────

    public List<String> listTopics() throws Exception {
        return await(admin.listTopics().names()).stream().sorted().toList();
    }

    public Map<String, Object> describeTopic(String name) throws Exception {
        TopicDescription description = await(admin.describeTopics(List.of(name)).allTopicNames()).get(name);
        List<TopicPartition> partitions = description.partitions().stream()
                .map(p -> new TopicPartition(name, p.partition()))
                .toList();
        Map<TopicPartition, Long> earliest = offsets(partitions, OffsetSpec.earliest());
        Map<TopicPartition, Long> latest = offsets(partitions, OffsetSpec.latest());

        int underReplicated = 0;
        List<Map<String, Object>> detail = new ArrayList<>();
        for (TopicPartitionInfo p : description.partitions()) {
            TopicPartition tp = new TopicPartition(name, p.partition());
            if (p.isr().size() < p.replicas().size()) {
                underReplicated++;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("partition", p.partition());
            info.put("leader", p.leader() != null ? p.leader().id() : null);
            info.put("replicas", p.replicas().stream().map(Node::id).toList());
            info.put("isr", p.isr().stream().map(Node::id).toList());
            info.put("offsetInicio", earliest.get(tp));
            info.put("offsetFin", latest.get(tp));
            detail.add(info);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("internal", description.isInternal());
        result.put("partitionCount", description.partitions().size());
        result.put("replicationFactor", description.partitions().isEmpty() ? 0 : description.partitions().get(0).replicas().size());
        result.put("underReplicatedPartitions", underReplicated);
        result.put("configs", getTopicConfig(name));
        result.put("partitions", detail);
        return result;
    }

    public void createTopic(CreateTopicRequest request) throws Exception {
        validateConfigKeys(request.configs());
        NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicationFactor().shortValue());
        if (request.configs() != null) {
            topic.configs(request.configs());
        }
        await(admin.createTopics(List.of(topic)).all());
        log.info("[KafkaAdmin] Tópico creado {} partitions={} replicas={} configs={}",
                request.name(), request.partitions(), request.replicationFactor(), request.configs());
    }

    public void deleteTopic(String name) throws Exception {
        if (name.startsWith("__")) {
            throw new IllegalArgumentException("No se pueden eliminar tópicos internos de Kafka: " + name);
        }
        await(admin.deleteTopics(List.of(name)).all());
        log.info("[KafkaAdmin] Tópico eliminado {}", name);
    }

    public void increasePartitions(String name, int total) throws Exception {
        await(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(total))).all());
        log.info("[KafkaAdmin] Tópico {} ahora tiene {} particiones", name, total);
    }

    // ── Configuración de tópicos ─────────────────────────────────────────────

    /** Configuraciones relevantes con su valor y su origen (propia del tópico o default del broker). */
    public Map<String, Object> getTopicConfig(String name) throws Exception {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = await(admin.describeConfigs(List.of(resource)).all()).get(resource);
        Map<String, Object> result = new TreeMap<>();
        for (ConfigEntry entry : config.entries()) {
            if (entry.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG || EDITABLE_CONFIGS.contains(entry.name())) {
                result.put(entry.name(), Map.of(
                        "valor", String.valueOf(entry.value()),
                        "origen", entry.source().name()));
            }
        }
        return result;
    }

    public void updateTopicConfig(String name, Map<String, String> changes) throws Exception {
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una configuración");
        }
        validateConfigKeys(changes);
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        List<AlterConfigOp> ops = changes.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        await(admin.incrementalAlterConfigs(Map.of(resource, ops)).all());
        log.info("[KafkaAdmin] Config actualizada topic={} cambios={}", name, changes);
    }

    /** Quita la configuración propia del tópico y vuelve al valor por defecto del broker. */
    public void resetTopicConfig(String name, String key) throws Exception {
        validateConfigKeys(Map.of(key, ""));
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        AlterConfigOp op = new AlterConfigOp(new ConfigEntry(key, ""), AlterConfigOp.OpType.DELETE);
        await(admin.incrementalAlterConfigs(Map.of(resource, List.of(op))).all());
        log.info("[KafkaAdmin] Config {} del tópico {} vuelve al valor por defecto", key, name);
    }

    // ── Consumer groups ──────────────────────────────────────────────────────

    public List<Map<String, Object>> listConsumerGroups() throws Exception {
        List<Map<String, Object>> groups = new ArrayList<>();
        for (GroupListing listing : await(admin.listGroups().all())) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("groupId", listing.groupId());
            group.put("estado", listing.groupState().map(Enum::name).orElse("DESCONOCIDO"));
            groups.add(group);
        }
        groups.sort(Comparator.comparing(g -> (String) g.get("groupId")));
        return groups;
    }

    /**
     * Estado del grupo, consumidores con sus particiones y, por cada partición:
     * offset confirmado, offset final del tópico y lag (mensajes pendientes por procesar).
     */
    public Map<String, Object> describeConsumerGroup(String groupId) throws Exception {
        ConsumerGroupDescription description =
                await(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        Map<TopicPartition, OffsetAndMetadata> committed =
                await(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, Long> endOffsets = committed.isEmpty()
                ? Map.of()
                : offsets(committed.keySet(), OffsetSpec.latest());

        Map<TopicPartition, String> owner = new HashMap<>();
        List<Map<String, Object>> members = new ArrayList<>();
        for (MemberDescription member : description.members()) {
            List<String> assigned = member.assignment().topicPartitions().stream()
                    .sorted(BY_TOPIC_PARTITION)
                    .map(TopicPartition::toString)
                    .toList();
            member.assignment().topicPartitions().forEach(tp -> owner.put(tp, member.clientId()));
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("clientId", member.clientId());
            info.put("host", member.host());
            info.put("particiones", assigned);
            members.add(info);
        }

        long totalLag = 0;
        List<Map<String, Object>> partitions = new ArrayList<>();
        List<TopicPartition> sorted = committed.keySet().stream().sorted(BY_TOPIC_PARTITION).toList();
        for (TopicPartition tp : sorted) {
            OffsetAndMetadata offset = committed.get(tp);
            Long committedOffset = offset != null ? offset.offset() : null;
            Long end = endOffsets.get(tp);
            Long lag = committedOffset != null && end != null ? Math.max(0, end - committedOffset) : null;
            if (lag != null) {
                totalLag += lag;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("topic", tp.topic());
            info.put("partition", tp.partition());
            info.put("offsetConfirmado", committedOffset);
            info.put("offsetFinal", end);
            info.put("lag", lag);
            info.put("consumidor", owner.getOrDefault(tp, "-"));
            partitions.add(info);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId);
        result.put("estado", description.groupState().name());
        result.put("coordinador", description.coordinator() != null ? description.coordinator().id() : null);
        result.put("consumidoresActivos", description.members().size());
        result.put("lagTotal", totalLag);
        result.put("consumidores", members);
        result.put("particiones", partitions);
        return result;
    }

    /**
     * Mueve los offsets de un grupo. Kafka exige que el grupo no tenga consumidores activos,
     * por eso primero hay que detener sus listeners (ListenerControlService).
     */
    public Map<String, Object> resetConsumerGroupOffsets(String groupId, ResetOffsetsRequest request) throws Exception {
        ConsumerGroupDescription description =
                await(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        if (!description.members().isEmpty()) {
            throw new IllegalStateException("El grupo " + groupId + " tiene " + description.members().size()
                    + " consumidores activos. Primero detén su listener con POST /api/kafka/admin/listeners/{id}/stop");
        }

        String topic = request.topic();
        TopicDescription topicDescription = await(admin.describeTopics(List.of(topic)).allTopicNames()).get(topic);
        List<TopicPartition> partitions = topicDescription.partitions().stream()
                .map(p -> new TopicPartition(topic, p.partition()))
                .toList();

        String strategy = request.strategy().toLowerCase(Locale.ROOT);
        Map<TopicPartition, Long> targets = switch (strategy) {
            case "earliest" -> offsets(partitions, OffsetSpec.earliest());
            case "latest" -> offsets(partitions, OffsetSpec.latest());
            case "offset" -> {
                if (request.offset() == null) {
                    throw new IllegalArgumentException("Con strategy=offset hay que indicar el campo offset");
                }
                Map<TopicPartition, Long> fixed = new HashMap<>();
                partitions.forEach(tp -> fixed.put(tp, request.offset()));
                yield fixed;
            }
            default -> throw new IllegalArgumentException("strategy debe ser earliest, latest u offset");
        };

        Map<TopicPartition, OffsetAndMetadata> newOffsets = new HashMap<>();
        targets.forEach((tp, offset) -> newOffsets.put(tp, new OffsetAndMetadata(offset)));
        await(admin.alterConsumerGroupOffsets(groupId, newOffsets).all());
        log.info("[KafkaAdmin] Offsets del grupo {} en {} reseteados con strategy={}", groupId, topic, strategy);

        Map<String, Long> applied = new TreeMap<>();
        targets.forEach((tp, offset) -> applied.put(tp.toString(), offset));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId);
        result.put("topic", topic);
        result.put("strategy", strategy);
        result.put("nuevosOffsets", applied);
        return result;
    }

    public void deleteConsumerGroup(String groupId) throws Exception {
        await(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.info("[KafkaAdmin] Consumer group eliminado {}", groupId);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private Map<TopicPartition, Long> offsets(Collection<TopicPartition> partitions, OffsetSpec spec) throws Exception {
        Map<TopicPartition, OffsetSpec> request = new HashMap<>();
        partitions.forEach(tp -> request.put(tp, spec));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> response = await(admin.listOffsets(request).all());
        Map<TopicPartition, Long> result = new HashMap<>();
        response.forEach((tp, info) -> result.put(tp, info.offset()));
        return result;
    }

    private void validateConfigKeys(Map<String, String> configs) {
        if (configs == null) {
            return;
        }
        for (String key : configs.keySet()) {
            if (!EDITABLE_CONFIGS.contains(key)) {
                throw new IllegalArgumentException("Configuración no permitida: " + key
                        + ". Permitidas: " + String.join(", ", EDITABLE_CONFIGS.stream().sorted().toList()));
            }
        }
    }

    /** Espera el resultado del AdminClient con timeout, para que una petición nunca quede colgada. */
    private <T> T await(KafkaFuture<T> future) throws Exception {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}