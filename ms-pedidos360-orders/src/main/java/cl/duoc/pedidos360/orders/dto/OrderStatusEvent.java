package cl.duoc.pedidos360.orders.dto;

public class OrderStatusEvent {

    private Long orderId;
    private String cliente;
    private String estado;

    public OrderStatusEvent() {}

    public OrderStatusEvent(Long orderId, String cliente, String estado) {
        this.orderId = orderId;
        this.cliente = cliente;
        this.estado = estado;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}