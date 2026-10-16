package cl.duoc.pedidos360.rabbitmq.consumer.audit;

import cl.duoc.pedidos360.rabbitmq.consumer.support.MessageAck;
import cl.duoc.pedidos360.rabbitmq.dto.AuditEvent;
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

/**
 * Caso de uso 3: Auditoría de eventos.
 *
 * Consume mensajes de q.pedidos360.audit (routing: audit.#) y los persiste
 * en la tabla audit_logs de Supabase.
 */
@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final MessageAck acknowledger;

    public AuditConsumer(AuditLogRepository auditLogRepository,
                         MessageAck acknowledger) {
        this.auditLogRepository = auditLogRepository;
        this.acknowledger = acknowledger;
    }

    @RabbitListener(queues = "${pedidos360.rabbit.queues.audit}", ackMode = "MANUAL")
    public void handleAuditEvent(AuditEvent event,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                 @Header(AmqpHeaders.REDELIVERED) boolean redelivered)
            throws IOException {
        try {
            if (event.servicio() == null || event.accion() == null) {
                throw new IllegalArgumentException("Evento de auditoría sin servicio o acción");
            }

            log.info("[AUDITORIA] {} | {} | {} — {}",
                    event.servicio(), event.recurso(), event.accion(), event.detalle());
                    

            AuditLog logEntry = new AuditLog(
                    event.servicio(),
                    event.recurso(),
                    event.accion(),
                    event.detalle(),
                    event.timestamp()
            );
            auditLogRepository.save(logEntry);

            log.info("[AUDITORIA] Evento persistido — id: {}", logEntry.getId());
            acknowledger.ack(channel, deliveryTag);

        } catch (Exception e) {
            acknowledger.reject(channel, deliveryTag, redelivered, e, "AUDITORIA");
        }
    }
}