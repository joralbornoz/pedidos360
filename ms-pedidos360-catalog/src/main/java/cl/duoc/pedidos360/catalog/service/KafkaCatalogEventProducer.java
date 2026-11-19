package cl.duoc.pedidos360.catalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Productor Kafka de ms-pedidos360-catalog.
 * Publica eventos de cambio en productos al tópico pedidos360.catalog-events.
 * No contiene lógica de negocio — solo serializa y envía.
 */
@Service
public class KafkaCatalogEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaCatalogEventProducer.class);

    @Value("${pedidos360.kafka.topics.catalog-events}")
    private String catalogEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaCatalogEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Long productId, String nombre, String accion) {
        String key = "product-" + productId;
        Map<String, Object> event = Map.of(
                "productId", productId,
                "nombre", nombre,
                "accion", accion,
                "timestamp", LocalDateTime.now().toString()
        );
        kafkaTemplate.send(catalogEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka][Catalog] Error publicando evento productId={}: {}",
                                productId, ex.getMessage());
                    } else {
                        log.info("[Kafka][Catalog] Evento publicado — topic={} partition={} offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}