package cl.duoc.pedidos360.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ── Nombres de exchanges ──────────────────────────────────────────────────
    public static final String EXCHANGE = "pedidos360.exchange";
    public static final String EXCHANGE_DLX = "pedidos360.dlx";

    // ── Nombres de queues ─────────────────────────────────────────────────────
    public static final String Q_NOTIFICATIONS = "q.pedidos360.notifications";
    public static final String Q_STOCK = "q.pedidos360.stock";
    public static final String Q_AUDIT = "q.pedidos360.audit";

    // ── Dead-letter queues (una por cola) ─────────────────────────────────────
    public static final String Q_NOTIFICATIONS_DLQ = "q.pedidos360.notifications.dlq";
    public static final String Q_STOCK_DLQ = "q.pedidos360.stock.dlq";
    public static final String Q_AUDIT_DLQ = "q.pedidos360.audit.dlq";

    // ── Routing keys ──────────────────────────────────────────────────────────
    public static final String RK_STATUS = "order.status.changed";
    public static final String RK_STOCK = "order.stock.decrement";
    public static final String RK_AUDIT = "audit.#";

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange mainExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLX).durable(true).build();
    }

    // ── Queues principales ────────────────────────────────────────────────────

    private Map<String, Object> dlqArgs(String dlqRoutingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        return args;
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(Q_NOTIFICATIONS)
                .withArguments(dlqArgs(Q_NOTIFICATIONS_DLQ))
                .build();
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(Q_STOCK)
                .withArguments(dlqArgs(Q_STOCK_DLQ))
                .build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(Q_AUDIT)
                .withArguments(dlqArgs(Q_AUDIT_DLQ))
                .build();
    }

    // ── Dead-letter queues ────────────────────────────────────────────────────

    @Bean
    public Queue notificationsDlq() {
        return QueueBuilder.durable(Q_NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Queue stockDlq() {
        return QueueBuilder.durable(Q_STOCK_DLQ).build();
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(Q_AUDIT_DLQ).build();
    }

    // ── Bindings principales ──────────────────────────────────────────────────

    @Bean
    public Binding bindNotifications() {
        return BindingBuilder.bind(notificationsQueue())
                .to(mainExchange())
                .with("order.status.*");
    }

    @Bean
    public Binding bindStock() {
        return BindingBuilder.bind(stockQueue())
                .to(mainExchange())
                .with(RK_STOCK);
    }

    @Bean
    public Binding bindAudit() {
        return BindingBuilder.bind(auditQueue())
                .to(mainExchange())
                .with("audit.#");
    }

    // ── Bindings DLQ (una por cola) ───────────────────────────────────────────

    @Bean
    public Binding bindNotificationsDlq() {
        return BindingBuilder.bind(notificationsDlq())
                .to(deadLetterExchange())
                .with(Q_NOTIFICATIONS_DLQ);
    }

    @Bean
    public Binding bindStockDlq() {
        return BindingBuilder.bind(stockDlq())
                .to(deadLetterExchange())
                .with(Q_STOCK_DLQ);
    }

    @Bean
    public Binding bindAuditDlq() {
        return BindingBuilder.bind(auditDlq())
                .to(deadLetterExchange())
                .with(Q_AUDIT_DLQ);
    }

    // ── Serialización JSON ────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}