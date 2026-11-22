package cl.duoc.pedidos360.catalog.dto;

import cl.duoc.pedidos360.catalog.entity.Product;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento que ms-pedidos360-catalog publica en el tópico pedidos360.catalog-events.
 * Lleva el estado actual del producto (precio y stock), así el tópico compactado
 * guarda la "foto" más reciente de cada producto.
 */
public record CatalogEvent(
        String eventId,
        String eventType,
        Long productId,
        String nombre,
        Double precio,
        Integer stock,
        Instant timestamp,
        String source) {

    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String PRODUCT_DELETED = "PRODUCT_DELETED";
    public static final String STOCK_DECREASED = "STOCK_DECREASED";

    private static final String SOURCE = "ms-pedidos360-catalog";

    public static CatalogEvent of(String eventType, Product product) {
        return new CatalogEvent(
                UUID.randomUUID().toString(),
                eventType,
                product.getId(),
                product.getNombre(),
                product.getPrecio(),
                product.getStock(),
                Instant.now(),
                SOURCE);
    }
}