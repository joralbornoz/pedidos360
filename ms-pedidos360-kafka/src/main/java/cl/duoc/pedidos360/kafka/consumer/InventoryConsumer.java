package cl.duoc.pedidos360.kafka.consumer;

import cl.duoc.pedidos360.kafka.consumer.support.EventValidator;
import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import cl.duoc.pedidos360.kafka.service.streaming.InventoryMonitorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del tópico pedidos360.catalog-events.
 *
 * Consumer group: pedidos360-catalog-inventory
 * concurrency = 3 → un hilo por partición.
 * La lógica (stock actual y alertas de stock bajo) está en InventoryMonitorService.
 */
@Component
public class InventoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryConsumer.class);

    private final InventoryMonitorService inventoryService;
    private final EventValidator validator;

    public InventoryConsumer(InventoryMonitorService inventoryService, EventValidator validator) {
        this.inventoryService = inventoryService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "catalog-inventory-listener",
            topics = "${pedidos360.kafka.topics.catalog-events}",
            groupId = "${pedidos360.kafka.consumer-groups.catalog-inventory}",
            concurrency = "${pedidos360.kafka.concurrency.catalog-inventory}",
            containerFactory = "catalogEventListenerFactory"
    )
    public void consume(ConsumerRecord<String, CatalogEventDto> record) {
        // Si el payload no cumple el contrato, lanza InvalidEventException → DLT sin reintentos
        CatalogEventDto event = validator.validate(record.value());
        log.info("[Kafka][Inventario] {} productId={} stock={} partition={} offset={}",
                event.getEventType(), event.getProductId(), event.getStock(), record.partition(), record.offset());
        inventoryService.process(event);
    }
}