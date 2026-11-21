package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.dto.OrderStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Productor Kafka de ms-pedidos360-orders.
 * Publica eventos de cambio de estado al tópico pedidos360.order-events.
 * No contiene lógica de negocio — solo serializa y envía.
 */
@Service
public class KafkaOrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventProducer.class);

    @Value("${pedidos360.kafka.topics.order-events}")
    private String orderEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaOrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderStatusEvent event) {
        String key = "order-" + event.getOrderId();
        kafkaTemplate.send(orderEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka][Orders] Error publicando evento orderId={}: {}",
                                event.getOrderId(), ex.getMessage());
                    } else {
                        log.info("[Kafka][Orders] Evento publicado — topic={} partition={} offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}