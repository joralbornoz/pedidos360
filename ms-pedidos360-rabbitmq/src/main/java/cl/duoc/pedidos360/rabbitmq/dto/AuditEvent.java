package cl.duoc.pedidos360.rabbitmq.dto;

import java.time.Instant;

public record AuditEvent(
        String servicio,
        String recurso,
        String accion,
        String detalle,
        Instant timestamp) {
    public static AuditEvent of(String servicio, String recurso,
            String accion, String detalle) {
        return new AuditEvent(servicio, recurso, accion, detalle, Instant.now());
    }
}
