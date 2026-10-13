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

import java.util.HashMap;
import java.util.Map;

/**
 * Topología RabbitMQ para Pedidos360.
 *
 * Exchange principal : pedidos360.exchange  (topic)
 * Exchange DLX       : pedidos360.dlx       (direct)
 *
 * Queues:
 *   q.pedidos360.notifications  ← routing key: order.status.*
 *   q.pedidos360.stock          ← routing key: order.stock.decrement
 *   q.pedidos360.audit          ← routing key: audit.#
 *   q.pedidos360.dlq            ← dead-letter de las tres anteriores
 */
@Configuration
public class RabbitMQConfig {

    // ── Nombres de exchanges ──────────────────────────────────────────────────
    public static final String EXCHANGE        = "pedidos360.exchange";
    public static final String EXCHANGE_DLX    = "pedidos360.dlx";

    // ── Nombres de queues ─────────────────────────────────────────────────────
    public static final String Q_NOTIFICATIONS = "q.pedidos360.notifications";
    public static final String Q_STOCK         = "q.pedidos360.stock";
    public static final String Q_AUDIT         = "q.pedidos360.audit";
    public static final String Q_DLQ           = "q.pedidos360.dlq";

    // ── Routing keys ──────────────────────────────────────────────────────────
    public static final String RK_STATUS   = "order.status.changed";
    public static final String RK_STOCK    = "order.stock.decrement";
    public static final String RK_AUDIT    = "audit.#";

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange mainExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLX).durable(true).build();
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    private Map<String, Object> dlqArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        return args;
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(Q_NOTIFICATIONS).withArguments(dlqArgs()).build();
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(Q_STOCK).withArguments(dlqArgs()).build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(Q_AUDIT).withArguments(dlqArgs()).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(Q_DLQ).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

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

    @Bean
    public Binding bindDlq() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(Q_DLQ);
    }

    // ── Serialización JSON ────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
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
