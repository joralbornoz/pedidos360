package cl.duoc.pedidos360.orders.dto;

import cl.duoc.pedidos360.orders.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento que ms-pedidos360-orders publica en el tópico pedidos360.order-events.
 * Es inmutable (record) y se arma con los métodos de fábrica según lo que pasó con el pedido.
 */
public record OrderEvent(
        String eventId,
        String eventType,
        Long orderId,
        String cliente,
        String estadoAnterior,
        String estadoNuevo,
        Instant timestamp,
        String source) {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String ORDER_DELETED = "ORDER_DELETED";

    private static final String SOURCE = "ms-pedidos360-orders";

    public static OrderEvent created(Order order) {
        return of(ORDER_CREATED, order, null, order.getEstado());
    }

    public static OrderEvent statusChanged(Order order, String estadoAnterior) {
        return of(ORDER_STATUS_CHANGED, order, estadoAnterior, order.getEstado());
    }

    public static OrderEvent deleted(Order order) {
        return of(ORDER_DELETED, order, order.getEstado(), "ELIMINADO");
    }

    private static OrderEvent of(String type, Order order, String estadoAnterior, String estadoNuevo) {
        return new OrderEvent(
                UUID.randomUUID().toString(),
                type,
                order.getId(),
                order.getCliente(),
                estadoAnterior,
                estadoNuevo,
                Instant.now(),
                SOURCE);
    }
}