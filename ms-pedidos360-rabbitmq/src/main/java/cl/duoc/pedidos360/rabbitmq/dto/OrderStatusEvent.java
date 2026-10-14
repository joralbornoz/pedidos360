package cl.duoc.pedidos360.rabbitmq.dto;

import java.time.Instant;

public record OrderStatusEvent(
        Long   orderId,
        String cliente,
        String estadoAnterior,
        String estadoNuevo,
        Instant timestamp
) {
    public static OrderStatusEvent of(Long orderId, String cliente,
                                      String estadoAnterior, String estadoNuevo) {
        return new OrderStatusEvent(orderId, cliente, estadoAnterior, estadoNuevo, Instant.now());
    }
}