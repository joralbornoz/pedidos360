package cl.duoc.pedidos360.catalog.service;

import cl.duoc.pedidos360.catalog.dto.CatalogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica los eventos del catálogo en el tópico pedidos360.catalog-events.
 * La key es product-{id}: los eventos de un producto van siempre a la misma
 * partición (orden garantizado) y la compactación conserva el último de cada uno.
 */
@Service
public class KafkaCatalogEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaCatalogEventProducer.class);

    private final KafkaTemplate<String, CatalogEvent> kafkaTemplate;
    private final String catalogEventsTopic;

    public KafkaCatalogEventProducer(KafkaTemplate<String, CatalogEvent> kafkaTemplate,
                                     @Value("${pedidos360.kafka.topics.catalog-events}") String catalogEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.catalogEventsTopic = catalogEventsTopic;
    }

    public void publish(CatalogEvent event) {
        String key = "product-" + event.productId();
        kafkaTemplate.send(catalogEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka][Catalog] Error publicando {} productId={} eventId={}: {}",
                                event.eventType(), event.productId(), event.eventId(), ex.getMessage());
                    } else {
                        log.info("[Kafka][Catalog] {} publicado — productId={} stock={} topic={} partition={} offset={}",
                                event.eventType(), event.productId(), event.stock(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}