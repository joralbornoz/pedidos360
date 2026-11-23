package cl.duoc.pedidos360.kafka.consumer;

import cl.duoc.pedidos360.kafka.consumer.support.EventValidator;
import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import cl.duoc.pedidos360.kafka.service.streaming.OrderTrackingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Segundo consumidor del MISMO tópico pedidos360.order-events, pero en otro grupo.
 *
 * Consumer group: pedidos360-orders-tracking
 * Como es un grupo distinto, recibe TODOS los eventos aunque el grupo de analítica
 * también los esté leyendo (modelo publicar/suscribir). Cada grupo lleva sus propios offsets.
 */
@Component
public class OrderTrackingConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderTrackingConsumer.class);

    private final OrderTrackingService trackingService;
    private final EventValidator validator;

    public OrderTrackingConsumer(OrderTrackingService trackingService, EventValidator validator) {
        this.trackingService = trackingService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "orders-tracking-listener",
            topics = "${pedidos360.kafka.topics.order-events}",
            groupId = "${pedidos360.kafka.consumer-groups.orders-tracking}",
            concurrency = "${pedidos360.kafka.concurrency.orders-tracking}",
            containerFactory = "orderEventListenerFactory"
    )
    public void consume(ConsumerRecord<String, OrderEventDto> record) {
        // Si el payload no cumple el contrato, lanza InvalidEventException → DLT sin reintentos
        OrderEventDto event = validator.validate(record.value());
        log.info("[Kafka][Tracking] {} orderId={} partition={} offset={}",
                event.getEventType(), event.getOrderId(), record.partition(), record.offset());
        trackingService.process(event);
    }
}