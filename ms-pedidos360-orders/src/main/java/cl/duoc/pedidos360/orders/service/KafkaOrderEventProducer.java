package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica los eventos de pedidos en el tópico pedidos360.order-events.
 * La key es order-{id}: todos los eventos de un mismo pedido caen en la misma
 * partición, así se procesan en el orden en que ocurrieron.
 * El envío es asíncrono: si Kafka falla, se registra el error pero no se cae la petición HTTP.
 */
@Service
public class KafkaOrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String orderEventsTopic;

    public KafkaOrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                                   @Value("${pedidos360.kafka.topics.order-events}") String orderEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsTopic = orderEventsTopic;
    }

    public void publish(OrderEvent event) {
        String key = "order-" + event.orderId();
        kafkaTemplate.send(orderEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka][Orders] Error publicando {} orderId={} eventId={}: {}",
                                event.eventType(), event.orderId(), event.eventId(), ex.getMessage());
                    } else {
                        log.info("[Kafka][Orders] {} publicado — orderId={} topic={} partition={} offset={}",
                                event.eventType(), event.orderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}