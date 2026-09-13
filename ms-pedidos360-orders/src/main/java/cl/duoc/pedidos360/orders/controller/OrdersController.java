package cl.duoc.pedidos360.orders.controller;
import cl.duoc.pedidos360.orders.entity.Order;
import cl.duoc.pedidos360.orders.repository.OrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
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
    
}