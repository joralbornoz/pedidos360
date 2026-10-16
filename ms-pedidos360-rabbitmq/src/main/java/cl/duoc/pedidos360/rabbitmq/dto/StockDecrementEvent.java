package cl.duoc.pedidos360.rabbitmq.dto;

import java.time.Instant;

public record StockDecrementEvent(
        Long orderId,
        Long productId,
        Integer cantidad,
        Instant timestamp) {
    public static StockDecrementEvent of(Long orderId, Long productId, Integer cantidad) {
        return new StockDecrementEvent(orderId, productId, cantidad, Instant.now());
    }
}
