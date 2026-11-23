package cl.duoc.pedidos360.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Evento del tópico pedidos360.order-events (lo publica ms-pedidos360-orders).
 *
 * eventType: ORDER_CREATED | ORDER_STATUS_CHANGED | ORDER_DELETED
 * estadoAnterior viene null cuando el pedido recién se crea.
 */
public class OrderEventDto {

    /** Id único del evento, sirve para detectar duplicados y rastrearlo en los logs. */
    private String eventId;

    @NotBlank
    private String eventType;

    @NotNull
    private Long orderId;

    @NotBlank
    private String cliente;

    private String estadoAnterior;

    @NotBlank
    private String estadoNuevo;

    private Instant timestamp;

    /** Microservicio que generó el evento. */
    private String source;

    public OrderEventDto() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}