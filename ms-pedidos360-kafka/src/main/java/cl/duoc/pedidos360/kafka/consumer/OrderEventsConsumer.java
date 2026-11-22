package cl.duoc.pedidos360.kafka.consumer;

import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka para el tópico pedidos360.order-events.
 * Consumer group: pedidos360-kafka-group.
 *
 * Lógica separada de la infraestructura de mensajería —
 * el procesamiento de negocio está en el método process().
 */
@Component
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    @KafkaListener(
            topics = "${pedidos360.kafka.topics.order-events}",
            groupId = "${pedidos360.kafka.consumer-groups.kafka-ms}",
            containerFactory = "orderEventListenerFactory"
    )
    public void consume(ConsumerRecord<String, OrderEventDto> record, Acknowledgment ack) {
        try {
            OrderEventDto event = record.value();

            if (event == null || event.getOrderId() == null) {
                log.warn("[Kafka][OrderEvents] Mensaje nulo o sin orderId — descartando. offset={}", record.offset());
                ack.acknowledge();
                return;
            }

            process(event, record.partition(), record.offset());
            ack.acknowledge(); // commit manual solo si procesó correctamente

        } catch (Exception ex) {
            log.error("[Kafka][OrderEvents] Error procesando mensaje offset={}: {}",
                    record.offset(), ex.getMessage());
            // No hace ack → Kafka reintentará según la política del listener
        }
    }

    /**
     * Lógica de negocio separada de la infraestructura.
     * El listener no sabe qué hace este método.
     */
    private void process(OrderEventDto event, int partition, long offset) {
        log.info("[Kafka][OrderEvents] Procesando — orderId={} cliente={} estado={}→{} partition={} offset={}",
                event.getOrderId(),
                event.getCliente(),
                event.getEstadoAnterior(),
                event.getEstadoNuevo(),
                partition,
                offset);
        // Aquí se podría extender: persistir en auditoría, disparar notificaciones, etc.
    }
}
