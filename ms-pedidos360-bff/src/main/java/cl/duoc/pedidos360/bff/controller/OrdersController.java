package cl.duoc.pedidos360.bff.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class OrdersController {
    @GetMapping("/api/orders")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public List<Map<String, Object>> getOrders() {
        return List.of(
                Map.of("id", 1, "cliente", "Juan Perez", "estado", "PENDIENTE"),
                Map.of("id", 2, "cliente", "Maria Soto", "estado", "ENTREGADO")
        );
    }
    
}
