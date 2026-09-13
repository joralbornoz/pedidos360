package cl.duoc.pedidos360.catalog.controller;

import cl.duoc.pedidos360.catalog.entity.Product;
import cl.duoc.pedidos360.catalog.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/api/catalog")
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @PutMapping("/api/catalog/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        product.setNombre(updatedProduct.getNombre());
        product.setPrecio(updatedProduct.getPrecio());
        product.setStock(updatedProduct.getStock());
        return productRepository.save(product);
    }

    @DeleteMapping("/api/catalog/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
    }

    @PatchMapping("/api/catalog/{id}/stock")
    public Product decreaseStock(@PathVariable Long id, @RequestParam Integer cantidad) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        if (product.getStock() < cantidad) {
            throw new IllegalStateException("Stock insuficiente");
        }
        product.setStock(product.getStock() - cantidad);
        return productRepository.save(product);
    }
}