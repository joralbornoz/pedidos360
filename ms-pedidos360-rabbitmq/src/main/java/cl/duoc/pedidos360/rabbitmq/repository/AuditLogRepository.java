package cl.duoc.pedidos360.rabbitmq.repository;

import cl.duoc.pedidos360.rabbitmq.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByServicioOrderByTimestampDesc(String servicio);

    List<AuditLog> findByRecursoAndAccionOrderByTimestampDesc(String recurso, String accion);
}
