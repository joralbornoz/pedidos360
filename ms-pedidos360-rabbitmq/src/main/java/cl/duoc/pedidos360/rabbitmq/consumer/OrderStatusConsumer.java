package cl.duoc.pedidos360.rabbitmq.consumer;

import cl.duoc.pedidos360.rabbitmq.config.RabbitMQConfig;
import cl.duoc.pedidos360.rabbitmq.dto.OrderStatusEvent;
import cl.duoc.pedidos360.rabbitmq.entity.AuditLog;
import cl.duoc.pedidos360.rabbitmq.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusConsumer.class);

    private final AuditLogRepository auditLogRepository;

    public OrderStatusConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATIONS)
    public void handleStatusChanged(OrderStatusEvent event) {
        log.info("[NOTIFICACION] Pedido #{} — cliente: '{}' | {} → {}  ({})",
                event.orderId(),
                event.cliente(),
                event.estadoAnterior() != null ? event.estadoAnterior() : "NUEVO",
                event.estadoNuevo(),
                event.timestamp());

        procesarNotificacion(event);

        AuditLog audit = new AuditLog(
                "ms-pedidos360-orders",
                "pedido/" + event.orderId(),
                "STATUS_CHANGED",
                "Cliente: " + event.cliente() + " | " +
                (event.estadoAnterior() != null ? event.estadoAnterior() : "NUEVO") +
                " → " + event.estadoNuevo(),
                event.timestamp() != null ? event.timestamp() : Instant.now()
        );
        auditLogRepository.save(audit);
        log.info("[AUDIT] Registro guardado para pedido #{}", event.orderId());
    }

    private void procesarNotificacion(OrderStatusEvent event) {
        log.info("[NOTIFICACION] Notificación enviada a cliente '{}': su pedido #{} ahora está en estado '{}'",
                event.cliente(), event.orderId(), event.estadoNuevo());
    }
}