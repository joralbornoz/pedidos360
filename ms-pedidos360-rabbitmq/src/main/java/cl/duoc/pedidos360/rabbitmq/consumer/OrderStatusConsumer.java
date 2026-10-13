package cl.duoc.pedidos360.rabbitmq.consumer;

import cl.duoc.pedidos360.rabbitmq.config.RabbitMQConfig;
import cl.duoc.pedidos360.rabbitmq.dto.OrderStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Caso de uso 1: Notificación al cambiar estado de pedido.
 *
 * Consume mensajes de q.pedidos360.notifications (routing: order.status.*).
 * En una implementación real podría enviar email/SMS/push; aquí loguea
 * y simula el procesamiento.
 */
@Component
public class OrderStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATIONS)
    public void handleStatusChanged(OrderStatusEvent event) {
        log.info("[NOTIFICACION] Pedido #{} — cliente: '{}' | {} → {}  ({})",
                event.orderId(),
                event.cliente(),
                event.estadoAnterior() != null ? event.estadoAnterior() : "NUEVO",
                event.estadoNuevo(),
                event.timestamp());

        // Aquí iría la lógica real: email, push notification, etc.
        procesarNotificacion(event);
    }

    private void procesarNotificacion(OrderStatusEvent event) {
        // Simulación de envío de notificación al cliente
        log.info("[NOTIFICACION] Notificación enviada a cliente '{}': su pedido #{} ahora está en estado '{}'",
                event.cliente(), event.orderId(), event.estadoNuevo());
    }
}
