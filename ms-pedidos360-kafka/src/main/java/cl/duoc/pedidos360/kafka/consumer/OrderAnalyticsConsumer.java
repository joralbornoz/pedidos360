package cl.duoc.pedidos360.kafka.consumer;

import cl.duoc.pedidos360.kafka.consumer.support.EventValidator;
import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import cl.duoc.pedidos360.kafka.service.streaming.OrderAnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico pedidos360.order-events para analítica en tiempo real.
 *
 * Consumer group: pedidos360-orders-analytics
 * concurrency = 3 → 3 hilos, uno por partición: el grupo reparte la carga
 * y si un hilo se cae sus particiones pasan a los otros.
 *
 * Solo se encarga de la mensajería; la lógica está en OrderAnalyticsService.
 */
@Component
public class OrderAnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderAnalyticsConsumer.class);

    private final OrderAnalyticsService analyticsService;
    private final EventValidator validator;

    public OrderAnalyticsConsumer(OrderAnalyticsService analyticsService, EventValidator validator) {
        this.analyticsService = analyticsService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "orders-analytics-listener",
            topics = "${pedidos360.kafka.topics.order-events}",
            groupId = "${pedidos360.kafka.consumer-groups.orders-analytics}",
            concurrency = "${pedidos360.kafka.concurrency.orders-analytics}",
            containerFactory = "orderEventListenerFactory"
    )
    public void consume(ConsumerRecord<String, OrderEventDto> record) {
        // Si el payload no cumple el contrato, lanza InvalidEventException → DLT sin reintentos
        OrderEventDto event = validator.validate(record.value());
        log.info("[Kafka][Analytics] {} orderId={} key={} partition={} offset={}",
                event.getEventType(), event.getOrderId(), record.key(), record.partition(), record.offset());
        analyticsService.process(event);
    }
}