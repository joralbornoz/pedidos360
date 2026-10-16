package cl.duoc.pedidos360.rabbitmq.service;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public void createQueue(String name, boolean durable) {
        rabbitAdmin.declareQueue(new Queue(name, durable));
    }

    public void deleteQueue(String name) {
        rabbitAdmin.deleteQueue(name);
    }

    public void createExchange(String name, String type, boolean durable) {
        AbstractExchange exchange = switch (type.toLowerCase()) {
            case "direct" -> new DirectExchange(name, durable, false);
            case "fanout" -> new FanoutExchange(name, durable, false);
            case "headers" -> new HeadersExchange(name, durable, false);
            default -> new TopicExchange(name, durable, false);
        };
        rabbitAdmin.declareExchange(exchange);
    }

    public void deleteExchange(String name) {
        rabbitAdmin.deleteExchange(name);
    }

    public void createBinding(String queueName, String exchangeName, String routingKey) {
        Binding binding = BindingBuilder
                .bind(new Queue(queueName))
                .to(new TopicExchange(exchangeName))
                .with(routingKey);
        rabbitAdmin.declareBinding(binding);
    }

    public void deleteBinding(String queueName, String exchangeName, String routingKey) {
        Binding binding = BindingBuilder
                .bind(new Queue(queueName))
                .to(new TopicExchange(exchangeName))
                .with(routingKey);
        rabbitAdmin.removeBinding(binding);
    }
}