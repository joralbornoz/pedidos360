package cl.duoc.pedidos360.kafka.controller;

import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import cl.duoc.pedidos360.kafka.producer.KafkaEventProducer;
import cl.duoc.pedidos360.kafka.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API de administración de Kafka.
 * Permite gestionar tópicos, consumer groups y publicar eventos de prueba.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService adminService;
    private final KafkaEventProducer producer;

    public KafkaAdminController(KafkaAdminService adminService, KafkaEventProducer producer) {
        this.adminService = adminService;
        this.producer = producer;
    }

    // ── Tópicos ───────────────────────────────────────────────────────────────

    @GetMapping("/topics")
    public ResponseEntity<Set<String>> listTopics() throws Exception {
        return ResponseEntity.ok(adminService.listTopics());
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, String>> createTopic(
            @RequestParam String name,
            @RequestParam(defaultValue = "3") int partitions,
            @RequestParam(defaultValue = "1") short replicationFactor,
            @RequestParam(defaultValue = "604800000") long retentionMs,
            @RequestParam(defaultValue = "delete") String cleanupPolicy) throws Exception {

        adminService.createTopic(name, partitions, replicationFactor, retentionMs, cleanupPolicy);
        return ResponseEntity.ok(Map.of("mensaje", "Tópico creado: " + name));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Map<String, String>> deleteTopic(@PathVariable String name) throws Exception {
        adminService.deleteTopic(name);
        return ResponseEntity.ok(Map.of("mensaje", "Tópico eliminado: " + name));
    }

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<Map<String, String>> updateTopicConfig(
            @PathVariable String name,
            @RequestParam long retentionMs,
            @RequestParam(defaultValue = "delete") String cleanupPolicy) throws Exception {

        adminService.updateTopicConfig(name, retentionMs, cleanupPolicy);
        return ResponseEntity.ok(Map.of("mensaje", "Config actualizada para: " + name));
    }

    @GetMapping("/topics/{name}/config")
    public ResponseEntity<Map<String, String>> getTopicConfig(@PathVariable String name) throws Exception {
        return ResponseEntity.ok(adminService.getTopicConfig(name));
    }

    // ── Consumer Groups ───────────────────────────────────────────────────────

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<String>> listConsumerGroups() throws Exception {
        return ResponseEntity.ok(adminService.listConsumerGroups());
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Map<String, Object>> describeConsumerGroup(
            @PathVariable String groupId) throws Exception {
        return ResponseEntity.ok(adminService.describeConsumerGroup(groupId));
    }

    // ── Publicar eventos (prueba / integración) ───────────────────────────────

    @PostMapping("/publish/order-event")
    public ResponseEntity<Map<String, String>> publishOrderEvent(
            @Valid @RequestBody OrderEventDto event) {

        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        producer.publishOrderEvent(event);
        return ResponseEntity.ok(Map.of("mensaje", "OrderEvent publicado para orderId=" + event.getOrderId()));
    }

    @PostMapping("/publish/catalog-event")
    public ResponseEntity<Map<String, String>> publishCatalogEvent(
            @Valid @RequestBody CatalogEventDto event) {

        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        producer.publishCatalogEvent(event);
        return ResponseEntity.ok(Map.of("mensaje", "CatalogEvent publicado para productId=" + event.getProductId()));
    }
}