package cl.duoc.pedidos360.rabbitmq.controller;

import cl.duoc.pedidos360.rabbitmq.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeRequest;
import cl.duoc.pedidos360.rabbitmq.dto.QueueRequest;
import cl.duoc.pedidos360.rabbitmq.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<String> createQueue(@Valid @RequestBody QueueRequest request) {
        rabbitAdminService.createQueue(request.getName(), request.isDurable());
        return ResponseEntity.status(HttpStatus.CREATED).body("Cola creada: " + request.getName());
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<String> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.ok("Cola eliminada: " + name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<String> createExchange(@Valid @RequestBody ExchangeRequest request) {
        rabbitAdminService.createExchange(request.getName(), request.getType(), request.isDurable());
        return ResponseEntity.status(HttpStatus.CREATED).body("Exchange creado: " + request.getName());
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<String> deleteExchange(@PathVariable String name) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.ok("Exchange eliminado: " + name);
    }

    @PostMapping("/bindings")
    public ResponseEntity<String> createBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.createBinding(request.getQueueName(), request.getExchangeName(), request.getRoutingKey());
        return ResponseEntity.status(HttpStatus.CREATED).body("Binding creado");
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<String> deleteBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.deleteBinding(request.getQueueName(), request.getExchangeName(), request.getRoutingKey());
        return ResponseEntity.ok("Binding eliminado");
    }
}