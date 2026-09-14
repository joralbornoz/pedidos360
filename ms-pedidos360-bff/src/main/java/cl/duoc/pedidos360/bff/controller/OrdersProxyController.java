package cl.duoc.pedidos360.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
public class OrdersProxyController {

    private final RestClient restClient;

    public OrdersProxyController(@Value("${orders.service.url}") String ordersServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(ordersServiceUrl)
                .build();
    }

    @GetMapping("/api/orders")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public List<Map<String, Object>> getOrders() {
        return restClient.get()
                .uri("/api/orders")
                .retrieve()
                .body(List.class);
    }

    @GetMapping("/api/orders/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public Map<String, Object> getOrderById(@PathVariable Long id) {
        return restClient.get()
                .uri("/api/orders/" + id)
                .retrieve()
                .body(Map.class);
    }

    @PatchMapping("/api/orders/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestParam String estado) {
        return restClient.patch()
                .uri("/api/orders/" + id + "/status?estado=" + estado)
                .retrieve()
                .body(Map.class);
    }
}