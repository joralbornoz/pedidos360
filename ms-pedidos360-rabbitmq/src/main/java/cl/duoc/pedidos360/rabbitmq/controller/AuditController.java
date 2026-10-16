package cl.duoc.pedidos360.rabbitmq.controller;

import cl.duoc.pedidos360.rabbitmq.entity.AuditLog;
import cl.duoc.pedidos360.rabbitmq.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Lista todos los eventos auditados (más recientes primero) */
    @GetMapping
    public List<AuditLog> getAll() {
        return auditLogRepository.findAll().stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();
    }

    /** Filtra por servicio origen */
    @GetMapping("/servicio/{servicio}")
    public List<AuditLog> getByServicio(@PathVariable String servicio) {
        return auditLogRepository.findByServicioOrderByTimestampDesc(servicio);
    }

    /** Filtra por recurso + acción */
    @GetMapping("/recurso/{recurso}/accion/{accion}")
    public List<AuditLog> getByRecursoAndAccion(
            @PathVariable String recurso,
            @PathVariable String accion) {
        return auditLogRepository.findByRecursoAndAccionOrderByTimestampDesc(recurso, accion);
    }
}
