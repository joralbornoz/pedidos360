package cl.duoc.pedidos360.orders.controller;
import cl.duoc.pedidos360.orders.entity.Order;
import cl.duoc.pedidos360.orders.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class OrdersController {
    private final OrderRepository orderRepository;
    public OrdersController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/orders")
    public List<Order> getOrders(){
        return orderRepository.findAll();
    }

    @GetMapping("/api/orders/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    @PostMapping("/api/orders")
    public Order createOrder(@RequestBody Order order) {
        return orderRepository.save(order);
    }

    @PutMapping("/api/orders/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order updatedOrder) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        order.setCliente(updatedOrder.getCliente());
        order.setEstado(updatedOrder.getEstado());
        return orderRepository.save(order);
    }

    @DeleteMapping("/api/orders/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }

    @PatchMapping("/api/orders/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestParam String estado) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        order.setEstado(estado);
        return orderRepository.save(order);
    }
}