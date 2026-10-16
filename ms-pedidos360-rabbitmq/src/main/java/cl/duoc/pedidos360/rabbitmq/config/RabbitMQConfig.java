package cl.duoc.pedidos360.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Topología RabbitMQ de Pedidos360.
 *
 * Todos los nombres (exchanges, colas, DLQs y routing keys) se leen desde
 * application.yml (prefijo pedidos360.rabbit.*). Aquí no hay nombres escritos a mano.
 *
 * Exchange principal : topic   (pedidos360.exchange)
 * Exchange DLX       : direct  (pedidos360.dlx)
 *
 * Ruta 1 - Notificaciones : exchange --(order.status.*)-------> q.notifications
 * Ruta 2 - Stock          : exchange --(order.stock.decrement)-> q.stock
 * Ruta 3 - Auditoría      : exchange --(audit.#)--------------> q.audit
 *
 * Dead letter: cada cola principal manda sus mensajes rechazados al DLX con la
 * routing key de su propia DLQ (q.notifications.dlq, q.stock.dlq, q.audit.dlq).
 */
@Configuration
public class RabbitMQConfig {

    // ── Nombres leídos desde application.yml ──────────────────────────────────
    @Value("${pedidos360.rabbit.exchange}")
    private String exchange;

    @Value("${pedidos360.rabbit.dlx}")
    private String dlx;

    @Value("${pedidos360.rabbit.queues.notifications}")
    private String qNotifications;

    @Value("${pedidos360.rabbit.queues.stock}")
    private String qStock;

    @Value("${pedidos360.rabbit.queues.audit}")
    private String qAudit;

    @Value("${pedidos360.rabbit.dlqs.notifications}")
    private String qNotificationsDlq;

    @Value("${pedidos360.rabbit.dlqs.stock}")
    private String qStockDlq;

    @Value("${pedidos360.rabbit.dlqs.audit}")
    private String qAuditDlq;

    @Value("${pedidos360.rabbit.routing-keys.status-pattern}")
    private String rkStatusPattern;

    @Value("${pedidos360.rabbit.routing-keys.stock-decrement}")
    private String rkStockDecrement;

    @Value("${pedidos360.rabbit.routing-keys.audit-pattern}")
    private String rkAuditPattern;

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange mainExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(dlx).durable(true).build();
    }

    // ── Queues principales ────────────────────────────────────────────────────

    /** Al ser rechazado, el mensaje sale por el DLX con la routing key de su DLQ. */
    private Map<String, Object> dlqArgs(String dlqRoutingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlx);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        return args;
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(qNotifications)
                .withArguments(dlqArgs(qNotificationsDlq))
                .build();
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(qStock)
                .withArguments(dlqArgs(qStockDlq))
                .build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(qAudit)
                .withArguments(dlqArgs(qAuditDlq))
                .build();
    }

    // ── Dead-letter queues ────────────────────────────────────────────────────

    @Bean
    public Queue notificationsDlq() {
        return QueueBuilder.durable(qNotificationsDlq).build();
    }

    @Bean
    public Queue stockDlq() {
        return QueueBuilder.durable(qStockDlq).build();
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(qAuditDlq).build();
    }

    // ── Bindings principales ──────────────────────────────────────────────────

    @Bean
    public Binding bindNotifications() {
        return BindingBuilder.bind(notificationsQueue())
                .to(mainExchange())
                .with(rkStatusPattern);
    }

    @Bean
    public Binding bindStock() {
        return BindingBuilder.bind(stockQueue())
                .to(mainExchange())
                .with(rkStockDecrement);
    }

    @Bean
    public Binding bindAudit() {
        return BindingBuilder.bind(auditQueue())
                .to(mainExchange())
                .with(rkAuditPattern);
    }

    // ── Bindings DLQ (una por cola) ───────────────────────────────────────────

    @Bean
    public Binding bindNotificationsDlq() {
        return BindingBuilder.bind(notificationsDlq())
                .to(deadLetterExchange())
                .with(qNotificationsDlq);
    }

    @Bean
    public Binding bindStockDlq() {
        return BindingBuilder.bind(stockDlq())
                .to(deadLetterExchange())
                .with(qStockDlq);
    }

    @Bean
    public Binding bindAuditDlq() {
        return BindingBuilder.bind(auditDlq())
                .to(deadLetterExchange())
                .with(qAuditDlq);
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
