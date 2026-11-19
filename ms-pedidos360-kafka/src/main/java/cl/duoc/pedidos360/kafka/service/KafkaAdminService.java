package cl.duoc.pedidos360.kafka.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Servicio de administración de Kafka.
 * Permite crear, listar, modificar y eliminar tópicos,
 * y consultar consumer groups.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final KafkaAdmin kafkaAdmin;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    private AdminClient buildAdminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    // ── Tópicos ───────────────────────────────────────────────────────────────

    /**
     * Lista todos los tópicos del clúster.
     */
    public Set<String> listTopics() throws ExecutionException, InterruptedException {
        try (AdminClient client = buildAdminClient()) {
            return client.listTopics().names().get();
        }
    }

    /**
     * Crea un tópico con particiones, réplicas y retención configurables.
     */
    public void createTopic(String name, int partitions, short replicationFactor,
                            long retentionMs, String cleanupPolicy)
            throws ExecutionException, InterruptedException {

        NewTopic topic = new NewTopic(name, partitions, replicationFactor);
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
        topic.configs(configs);

        try (AdminClient client = buildAdminClient()) {
            client.createTopics(List.of(topic)).all().get();
            log.info("[KafkaAdmin] Tópico creado: {} partitions={} replicas={}", name, partitions, replicationFactor);
        }
    }

    /**
     * Elimina un tópico por nombre.
     */
    public void deleteTopic(String name) throws ExecutionException, InterruptedException {
        try (AdminClient client = buildAdminClient()) {
            client.deleteTopics(List.of(name)).all().get();
            log.info("[KafkaAdmin] Tópico eliminado: {}", name);
        }
    }

    /**
     * Modifica la retención y cleanup policy de un tópico existente.
     */
    public void updateTopicConfig(String name, long retentionMs, String cleanupPolicy)
            throws ExecutionException, InterruptedException {

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        List<AlterConfigOp> ops = List.of(
                new AlterConfigOp(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(retentionMs)), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG,
                        cleanupPolicy), AlterConfigOp.OpType.SET)
        );

        try (AdminClient client = buildAdminClient()) {
            client.incrementalAlterConfigs(Map.of(resource, ops)).all().get();
            log.info("[KafkaAdmin] Config actualizada: topic={} retentionMs={} cleanupPolicy={}",
                    name, retentionMs, cleanupPolicy);
        }
    }

    /**
     * Obtiene la configuración actual de un tópico.
     */
    public Map<String, String> getTopicConfig(String name)
            throws ExecutionException, InterruptedException {

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        try (AdminClient client = buildAdminClient()) {
            Config config = client.describeConfigs(List.of(resource))
                    .all().get().get(resource);
            Map<String, String> result = new LinkedHashMap<>();
            config.entries().forEach(e -> result.put(e.name(), e.value()));
            return result;
        }
    }

    // ── Consumer Groups ───────────────────────────────────────────────────────

    /**
     * Lista los consumer groups del clúster.
     */
    public List<String> listConsumerGroups() throws ExecutionException, InterruptedException {
        try (AdminClient client = buildAdminClient()) {
            return client.listConsumerGroups().all().get()
                    .stream()
                    .map(ConsumerGroupListing::groupId)
                    .toList();
        }
    }

    /**
     * Describe un consumer group: lag, offsets por partición.
     */
    public Map<String, Object> describeConsumerGroup(String groupId)
            throws ExecutionException, InterruptedException {

        try (AdminClient client = buildAdminClient()) {
            ConsumerGroupDescription desc = client
                    .describeConsumerGroups(List.of(groupId))
                    .all().get().get(groupId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("groupId", desc.groupId());
            result.put("state", desc.state().toString());
            result.put("members", desc.members().size());
            return result;
        }
    }
}
