package cl.duoc.pedidos360.rabbitmq.consumer;

import cl.duoc.pedidos360.rabbitmq.config.RabbitMQConfig;
import cl.duoc.pedidos360.rabbitmq.dto.AuditEvent;
import cl.duoc.pedidos360.rabbitmq.entity.AuditLog;
import cl.duoc.pedidos360.rabbitmq.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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

    public AuditConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.Q_AUDIT)
    public void handleAuditEvent(AuditEvent event) {
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
    }
}
