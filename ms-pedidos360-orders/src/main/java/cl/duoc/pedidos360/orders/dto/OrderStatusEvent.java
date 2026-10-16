package cl.duoc.pedidos360.orders.dto;

import java.time.Instant;

public class OrderStatusEvent {
    private Long orderId;
    private String cliente;
    private String estadoAnterior;
    private String estadoNuevo;
    private Instant timestamp;

    public OrderStatusEvent() {}

    public OrderStatusEvent(Long orderId, String cliente, String estadoAnterior, String estadoNuevo) {
        this.orderId = orderId;
        this.cliente = cliente;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.timestamp = Instant.now();
    }

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
}