package cl.duoc.pedidos360.kafka.controller;

import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import cl.duoc.pedidos360.kafka.dto.admin.CreateTopicRequest;
import cl.duoc.pedidos360.kafka.dto.admin.ResetOffsetsRequest;
import cl.duoc.pedidos360.kafka.producer.KafkaEventProducer;
import cl.duoc.pedidos360.kafka.service.KafkaAdminService;
import cl.duoc.pedidos360.kafka.service.ListenerControlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST de administración de Kafka:
 * clúster, tópicos, configuraciones, consumer groups, listeners y publicación de eventos de prueba.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService adminService;
    private final ListenerControlService listenerControl;
    private final KafkaEventProducer producer;

    public KafkaAdminController(KafkaAdminService adminService,
                                ListenerControlService listenerControl,
                                KafkaEventProducer producer) {
        this.adminService = adminService;
        this.listenerControl = listenerControl;
        this.producer = producer;
    }

    // ── Clúster ──────────────────────────────────────────────────────────────

    @GetMapping("/admin/cluster")
    public Map<String, Object> cluster() throws Exception {
        return adminService.clusterInfo();
    }

    // ── Tópicos ──────────────────────────────────────────────────────────────

    @GetMapping("/admin/topics")
    public List<String> listTopics() throws Exception {
        return adminService.listTopics();
    }

    @GetMapping("/admin/topics/{name}")
    public Map<String, Object> describeTopic(@PathVariable String name) throws Exception {
        return adminService.describeTopic(name);
    }

    @PostMapping("/admin/topics")
    public ResponseEntity<Map<String, Object>> createTopic(@Valid @RequestBody CreateTopicRequest request) throws Exception {
        adminService.createTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.describeTopic(request.name()));
    }

    @DeleteMapping("/admin/topics/{name}")
    public Map<String, String> deleteTopic(@PathVariable String name) throws Exception {
        adminService.deleteTopic(name);
        return Map.of("mensaje", "Tópico eliminado: " + name);
    }

    @PostMapping("/admin/topics/{name}/partitions")
    public Map<String, Object> increasePartitions(@PathVariable String name, @RequestParam int total) throws Exception {
        adminService.increasePartitions(name, total);
        return adminService.describeTopic(name);
    }

    // ── Configuración de tópicos ─────────────────────────────────────────────

    @GetMapping("/admin/topics/{name}/config")
    public Map<String, Object> getTopicConfig(@PathVariable String name) throws Exception {
        return adminService.getTopicConfig(name);
    }

    @PutMapping("/admin/topics/{name}/config")
    public Map<String, Object> updateTopicConfig(@PathVariable String name,
                                                 @RequestBody Map<String, String> changes) throws Exception {
        adminService.updateTopicConfig(name, changes);
        return adminService.getTopicConfig(name);
    }

    @DeleteMapping("/admin/topics/{name}/config/{key}")
    public Map<String, Object> resetTopicConfig(@PathVariable String name, @PathVariable String key) throws Exception {
        adminService.resetTopicConfig(name, key);
        return adminService.getTopicConfig(name);
    }

    // ── Consumer groups ──────────────────────────────────────────────────────

    @GetMapping("/admin/consumer-groups")
    public List<Map<String, Object>> listConsumerGroups() throws Exception {
        return adminService.listConsumerGroups();
    }

    @GetMapping("/admin/consumer-groups/{groupId}")
    public Map<String, Object> describeConsumerGroup(@PathVariable String groupId) throws Exception {
        return adminService.describeConsumerGroup(groupId);
    }

    @PostMapping("/admin/consumer-groups/{groupId}/reset-offsets")
    public Map<String, Object> resetOffsets(@PathVariable String groupId,
                                            @Valid @RequestBody ResetOffsetsRequest request) throws Exception {
        return adminService.resetConsumerGroupOffsets(groupId, request);
    }

    @DeleteMapping("/admin/consumer-groups/{groupId}")
    public Map<String, String> deleteConsumerGroup(@PathVariable String groupId) throws Exception {
        adminService.deleteConsumerGroup(groupId);
        return Map.of("mensaje", "Consumer group eliminado: " + groupId);
    }

    // ── Listeners del microservicio ──────────────────────────────────────────

    @GetMapping("/admin/listeners")
    public List<Map<String, Object>> listListeners() {
        return listenerControl.listListeners();
    }

    @PostMapping("/admin/listeners/{listenerId}/stop")
    public Map<String, Object> stopListener(@PathVariable String listenerId) {
        return listenerControl.stop(listenerId);
    }

    @PostMapping("/admin/listeners/{listenerId}/start")
    public Map<String, Object> startListener(@PathVariable String listenerId) {
        return listenerControl.start(listenerId);
    }

    // ── Publicar eventos ────────────────────────────────────

    @PostMapping("/publish/order-event")
    public ResponseEntity<Map<String, String>> publishOrderEvent(@Valid @RequestBody OrderEventDto event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        if (event.getSource() == null) {
            event.setSource("ms-pedidos360-kafka (prueba manual)");
        }
        producer.publishOrderEvent(event);
        return ResponseEntity.ok(Map.of("mensaje", "OrderEvent publicado para orderId=" + event.getOrderId()));
    }

    @PostMapping("/publish/catalog-event")
    public ResponseEntity<Map<String, String>> publishCatalogEvent(@Valid @RequestBody CatalogEventDto event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        if (event.getSource() == null) {
            event.setSource("ms-pedidos360-kafka (prueba manual)");
        }
        producer.publishCatalogEvent(event);
        return ResponseEntity.ok(Map.of("mensaje", "CatalogEvent publicado para productId=" + event.getProductId()));
    }
}