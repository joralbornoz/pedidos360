package cl.duoc.pedidos360.rabbitmq.dto;

import java.time.Instant;

/**
 * Evento publicado por ms-pedidos360-orders cuando un pedido cambia de estado.
 * Routing key: order.status.changed
 */
public record OrderStatusEvent(
        Long   orderId,
        String cliente,
        String estadoAnterior,
        String estadoNuevo,
        Instant timestamp
) {
    /** Constructor sin estadoAnterior (para creaciones) */
    public static OrderStatusEvent of(Long orderId, String cliente,
                                      String estadoAnterior, String estadoNuevo) {
        return new OrderStatusEvent(orderId, cliente, estadoAnterior, estadoNuevo, Instant.now());
    }
}
