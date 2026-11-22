package cl.duoc.pedidos360.kafka.consumer;

import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka para el tópico pedidos360.catalog-events.
 * Consumer group: pedidos360-kafka-group.
 */
@Component
public class CatalogEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventsConsumer.class);

    @KafkaListener(
            topics = "${pedidos360.kafka.topics.catalog-events}",
            groupId = "${pedidos360.kafka.consumer-groups.kafka-ms}",
            containerFactory = "catalogEventListenerFactory"
    )
    public void consume(ConsumerRecord<String, CatalogEventDto> record, Acknowledgment ack) {
        try {
            CatalogEventDto event = record.value();

            if (event == null || event.getProductId() == null) {
                log.warn("[Kafka][CatalogEvents] Mensaje nulo o sin productId — descartando. offset={}", record.offset());
                ack.acknowledge();
                return;
            }

            process(event, record.partition(), record.offset());
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("[Kafka][CatalogEvents] Error procesando mensaje offset={}: {}",
                    record.offset(), ex.getMessage());
        }
    }

    private void process(CatalogEventDto event, int partition, long offset) {
        log.info("[Kafka][CatalogEvents] Procesando — productId={} nombre={} accion={} partition={} offset={}",
                event.getProductId(),
                event.getNombre(),
                event.getAccion(),
                partition,
                offset);
    }
}
