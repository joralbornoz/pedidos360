package cl.duoc.pedidos360.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class OrderEventDto {

    @NotNull
    private Long orderId;

    @NotBlank
    private String cliente;

    @NotBlank
    private String estadoAnterior;

    @NotBlank
    private String estadoNuevo;

    private LocalDateTime timestamp;

    public OrderEventDto() {}

    public OrderEventDto(Long orderId, String cliente, String estadoAnterior,
                         String estadoNuevo, LocalDateTime timestamp) {
        this.orderId = orderId;
        this.cliente = cliente;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.timestamp = timestamp;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
