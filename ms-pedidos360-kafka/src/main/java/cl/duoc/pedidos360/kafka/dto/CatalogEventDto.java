package cl.duoc.pedidos360.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class CatalogEventDto {

    @NotNull
    private Long productId;

    @NotBlank
    private String nombre;

    @NotBlank
    private String accion;

    private Instant timestamp;

    public CatalogEventDto() {}

    public CatalogEventDto(Long productId, String nombre, String accion, Instant timestamp) {
        this.productId = productId;
        this.nombre = nombre;
        this.accion = accion;
        this.timestamp = timestamp;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}