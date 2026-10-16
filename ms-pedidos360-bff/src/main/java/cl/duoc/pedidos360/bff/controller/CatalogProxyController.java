package cl.duoc.pedidos360.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
public class CatalogProxyController {

    private final RestClient restClient;

    public CatalogProxyController(@Value("${catalog.service.url}") String catalogServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogServiceUrl)
                .build();
    }

    @GetMapping("/api/catalog")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public List<Map<String, Object>> getProducts() {
        return restClient.get()
                .uri("/api/catalog")
                .retrieve()
                .body(List.class);
    }

    @PostMapping("/api/catalog")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> product) {
        return restClient.post()
                .uri("/api/catalog")
                .body(product)
                .retrieve()
                .body(Map.class);
    }
    
    @PutMapping("/api/catalog/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public Map<String, Object> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> product) {
        return restClient.put()
                .uri("/api/catalog/{id}", id)
                .body(product)
                .retrieve()
                .body(Map.class);
    }

    @DeleteMapping("/api/catalog/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public void deleteProduct(@PathVariable Long id) {
        restClient.delete()
                .uri("/api/catalog/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}