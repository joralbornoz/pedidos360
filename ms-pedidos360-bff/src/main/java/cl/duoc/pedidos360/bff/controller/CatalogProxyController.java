package cl.duoc.pedidos360.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
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
}