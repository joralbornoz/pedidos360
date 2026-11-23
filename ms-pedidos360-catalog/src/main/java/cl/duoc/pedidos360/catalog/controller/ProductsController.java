package cl.duoc.pedidos360.catalog.controller;

import cl.duoc.pedidos360.catalog.dto.CatalogEvent;
import cl.duoc.pedidos360.catalog.entity.Product;
import cl.duoc.pedidos360.catalog.repository.ProductRepository;
import cl.duoc.pedidos360.catalog.service.KafkaCatalogEventProducer;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ProductsController {

    private final ProductRepository productRepository;
    private final KafkaCatalogEventProducer kafkaProducer;

    public ProductsController(ProductRepository productRepository,
                              KafkaCatalogEventProducer kafkaProducer) {
        this.productRepository = productRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping("/api/catalog")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @PostMapping("/api/catalog")
    public Product createProduct(@RequestBody Product product) {
        Product newProduct = new Product();
        newProduct.setNombre(product.getNombre());
        newProduct.setPrecio(product.getPrecio());
        newProduct.setStock(product.getStock());
        Product saved = productRepository.save(newProduct);
        kafkaProducer.publish(CatalogEvent.of(CatalogEvent.PRODUCT_CREATED, saved));
        return saved;
    }

    @PutMapping("/api/catalog/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        product.setNombre(updatedProduct.getNombre());
        product.setPrecio(updatedProduct.getPrecio());
        product.setStock(updatedProduct.getStock());
        Product saved = productRepository.save(product);
        kafkaProducer.publish(CatalogEvent.of(CatalogEvent.PRODUCT_UPDATED, saved));
        return saved;
    }

    @DeleteMapping("/api/catalog/{id}")
    public void deleteProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        productRepository.deleteById(id);
        kafkaProducer.publish(CatalogEvent.of(CatalogEvent.PRODUCT_DELETED, product));
    }

    @PatchMapping("/api/catalog/{id}/stock")
    public Product decreaseStock(@PathVariable Long id, @RequestParam Integer cantidad) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        if (product.getStock() < cantidad) {
            throw new IllegalStateException("Stock insuficiente");
        }
        product.setStock(product.getStock() - cantidad);
        Product saved = productRepository.save(product);
        kafkaProducer.publish(CatalogEvent.of(CatalogEvent.STOCK_DECREASED, saved));
        return saved;
    }
}