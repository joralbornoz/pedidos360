package cl.duoc.pedidos360.rabbitmq.consumer.notification;

import cl.duoc.pedidos360.rabbitmq.consumer.support.MessageAck;
import cl.duoc.pedidos360.rabbitmq.dto.OrderStatusEvent;
import cl.duoc.pedidos360.rabbitmq.entity.AuditLog;
import cl.duoc.pedidos360.rabbitmq.repository.AuditLogRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class OrderStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final MessageAck acknowledger;

    public OrderStatusConsumer(AuditLogRepository auditLogRepository,
                               MessageAck acknowledger) {
        this.auditLogRepository = auditLogRepository;
        this.acknowledger = acknowledger;
    }

    @RabbitListener(queues = "${pedidos360.rabbit.queues.notifications}", ackMode = "MANUAL")
    public void handleStatusChanged(OrderStatusEvent event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                    @Header(AmqpHeaders.REDELIVERED) boolean redelivered)
            throws IOException {
        try {
            if (event.orderId() == null || event.estadoNuevo() == null) {
                throw new IllegalArgumentException("Evento de pedido sin orderId o estadoNuevo");
            }

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

            acknowledger.ack(channel, deliveryTag);

        } catch (Exception e) {
            acknowledger.reject(channel, deliveryTag, redelivered, e, "NOTIFICACION");
        }
    }

    private void procesarNotificacion(OrderStatusEvent event) {
        log.info("[NOTIFICACION] Notificación enviada '{}': su pedido #{} estado '{}'",
                event.cliente(), event.orderId(), event.estadoNuevo());
    }
}