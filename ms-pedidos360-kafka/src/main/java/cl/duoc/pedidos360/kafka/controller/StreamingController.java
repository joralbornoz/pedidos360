package cl.duoc.pedidos360.kafka.controller;

import cl.duoc.pedidos360.kafka.service.streaming.DeadLetterService;
import cl.duoc.pedidos360.kafka.service.streaming.InventoryMonitorService;
import cl.duoc.pedidos360.kafka.service.streaming.OrderAnalyticsService;
import cl.duoc.pedidos360.kafka.service.streaming.OrderTrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Expone el resultado del procesamiento del stream:
 * lo que los consumidores van calculando a medida que llegan eventos.
 */
@RestController
@RequestMapping("/api/kafka/streaming")
public class StreamingController {

    private final OrderAnalyticsService analyticsService;
    private final OrderTrackingService trackingService;
    private final InventoryMonitorService inventoryService;
    private final DeadLetterService deadLetterService;

    public StreamingController(OrderAnalyticsService analyticsService,
                               OrderTrackingService trackingService,
                               InventoryMonitorService inventoryService,
                               DeadLetterService deadLetterService) {
        this.analyticsService = analyticsService;
        this.trackingService = trackingService;
        this.inventoryService = inventoryService;
        this.deadLetterService = deadLetterService;
    }

    /** KPIs de pedidos en tiempo real. */
    @GetMapping("/orders/kpis")
    public Map<String, Object> orderKpis() {
        return analyticsService.snapshot();
    }

    /** Historial de estados de un pedido. */
    @GetMapping("/orders/{orderId}/timeline")
    public List<OrderTrackingService.Transition> orderTimeline(@PathVariable Long orderId) {
        return trackingService.timeline(orderId);
    }

    /** Stock actual de cada producto y alertas de stock bajo. */
    @GetMapping("/inventory")
    public Map<String, Object> inventory() {
        return inventoryService.snapshot();
    }

    /** Últimos mensajes que terminaron en los DLT, con el motivo del error. */
    @GetMapping("/dlt")
    public List<DeadLetterService.DeadLetter> deadLetters() {
        return deadLetterService.recent();
    }
}