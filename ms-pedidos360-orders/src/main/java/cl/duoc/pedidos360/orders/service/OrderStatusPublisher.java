package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.dto.OrderStatusEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusPublisher {

    private static final String EXCHANGE = "pedidos360.exchange";
    private static final String ROUTING_KEY = "order.status.changed";

    private final RabbitTemplate rabbitTemplate;

    public OrderStatusPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OrderStatusEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}