package cl.duoc.pedidos360.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Evento del tópico pedidos360.catalog-events (lo publica ms-pedidos360-catalog).
 *
 * eventType: PRODUCT_CREATED | PRODUCT_UPDATED | PRODUCT_DELETED | STOCK_DECREASED
 * Trae el stock y precio actuales para que el consumidor no tenga que
 * consultar al microservicio de catálogo.
 */
public class CatalogEventDto {

    private String eventId;

    @NotBlank
    private String eventType;

    @NotNull
    private Long productId;

    @NotBlank
    private String nombre;

    @PositiveOrZero
    private Double precio;

    @PositiveOrZero
    private Integer stock;

    private Instant timestamp;

    private String source;

    public CatalogEventDto() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}