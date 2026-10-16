package cl.duoc.pedidos360.rabbitmq.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro persistido en la tabla audit_logs para cada evento de auditoría.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String servicio;

    @Column(nullable = false)
    private String recurso;

    @Column(nullable = false)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public AuditLog() {}

    public AuditLog(String servicio, String recurso, String accion,
                    String detalle, Instant timestamp) {
        this.servicio  = servicio;
        this.recurso   = recurso;
        this.accion    = accion;
        this.detalle   = detalle;
        this.timestamp = timestamp;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()          { return id; }
    public String getServicio()  { return servicio; }
    public String getRecurso()   { return recurso; }
    public String getAccion()    { return accion; }
    public String getDetalle()   { return detalle; }
    public Instant getTimestamp(){ return timestamp; }
    public Instant getCreatedAt(){ return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setServicio(String servicio)   { this.servicio = servicio; }
    public void setRecurso(String recurso)     { this.recurso = recurso; }
    public void setAccion(String accion)       { this.accion = accion; }
    public void setDetalle(String detalle)     { this.detalle = detalle; }
    public void setTimestamp(Instant timestamp){ this.timestamp = timestamp; }
}
