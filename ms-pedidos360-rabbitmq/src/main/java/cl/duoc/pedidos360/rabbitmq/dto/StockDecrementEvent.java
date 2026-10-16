package cl.duoc.pedidos360.rabbitmq.dto;

import java.time.Instant;

/**
 * Evento publicado por ms-pedidos360-orders cuando se acepta un pedido.
 * Indica qué producto y cuántas unidades descontar del stock.
 * Routing key: order.stock.decrement
 */
public record StockDecrementEvent(
        Long    orderId,
        Long    productId,
        Integer cantidad,
        Instant timestamp
) {
    public static StockDecrementEvent of(Long orderId, Long productId, Integer cantidad) {
        return new StockDecrementEvent(orderId, productId, cantidad, Instant.now());
    }
}
