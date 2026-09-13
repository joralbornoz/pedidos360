package cl.duoc.pedidos360.catalog.controller;

import cl.duoc.pedidos360.catalog.entity.Product;
import cl.duoc.pedidos360.catalog.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class ProductsController {
    private final ProductRepository productRepository;

    public ProductsController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/api/catalog")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
}