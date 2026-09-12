package cl.duoc.pedidos360.bff.controller;
import cl.duoc.pedidos360.bff.entity.Order;
import cl.duoc.pedidos360.bff.repository.OrderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public List<Order> getOrders(){
        return orderRepository.findAll();
    }
    
}
