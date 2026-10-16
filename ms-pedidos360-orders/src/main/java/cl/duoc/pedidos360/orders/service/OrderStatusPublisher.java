package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.dto.OrderStatusEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
/**
 * El exchange y la routing key se leen desde application.yml
 */
@Service
public class OrderStatusPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public OrderStatusPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${pedidos360.rabbit.exchange}") String exchange,
            @Value("${pedidos360.rabbit.routing-keys.status-changed}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(OrderStatusEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}