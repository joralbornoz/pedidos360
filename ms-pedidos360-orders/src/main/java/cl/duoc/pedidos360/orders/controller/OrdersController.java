package cl.duoc.pedidos360.orders.controller;

import cl.duoc.pedidos360.orders.dto.OrderEvent;
import cl.duoc.pedidos360.orders.dto.OrderStatusEvent;
import cl.duoc.pedidos360.orders.entity.Order;
import cl.duoc.pedidos360.orders.repository.OrderRepository;
import cl.duoc.pedidos360.orders.service.KafkaOrderEventProducer;
import cl.duoc.pedidos360.orders.service.OrderStatusPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class OrdersController {

    private final OrderRepository orderRepository;
    private final OrderStatusPublisher orderStatusPublisher;
    private final KafkaOrderEventProducer kafkaOrderEventProducer;

    public OrdersController(OrderRepository orderRepository,
                            OrderStatusPublisher orderStatusPublisher,
                            KafkaOrderEventProducer kafkaOrderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderStatusPublisher = orderStatusPublisher;
        this.kafkaOrderEventProducer = kafkaOrderEventProducer;
    }

    @GetMapping("/api/orders")
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/api/orders/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    @PostMapping("/api/orders")
    public Order createOrder(@RequestBody Order order) {
        Order saved = orderRepository.save(order);
        kafkaOrderEventProducer.publish(OrderEvent.created(saved));
        return saved;
    }

    @PutMapping("/api/orders/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order updatedOrder) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String estadoAnterior = order.getEstado();
        order.setCliente(updatedOrder.getCliente());
        order.setEstado(updatedOrder.getEstado());
        Order saved = orderRepository.save(order);

        // Solo se publica si de verdad cambió el estado
        if (!Objects.equals(estadoAnterior, saved.getEstado())) {
            kafkaOrderEventProducer.publish(OrderEvent.statusChanged(saved, estadoAnterior));
        }
        return saved;
    }

    @DeleteMapping("/api/orders/{id}")
    public void deleteOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        orderRepository.delete(order);
        kafkaOrderEventProducer.publish(OrderEvent.deleted(order));
    }

    @PatchMapping("/api/orders/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestParam String estado) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String estadoAnterior = order.getEstado();
        order.setEstado(estado);
        Order saved = orderRepository.save(order);

        // RabbitMQ: notificación del cambio de estado (EP3)
        orderStatusPublisher.publish(
                new OrderStatusEvent(saved.getId(), saved.getCliente(), estadoAnterior, estado));

        // Kafka: evento para el streaming y la analítica (EP5)
        kafkaOrderEventProducer.publish(OrderEvent.statusChanged(saved, estadoAnterior));

        return saved;
    }
}