package cl.duoc.pedidos360.kafka.producer;

import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Productor Kafka de Pedidos360.
 * Publica eventos tipados a los tópicos configurados.
 * No contiene lógica de negocio — solo serializa y envía.
 */
@Component
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);

    @Value("${pedidos360.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${pedidos360.kafka.topics.catalog-events}")
    private String catalogEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica un evento de cambio de estado de pedido.
     * La clave es el orderId para garantizar orden por pedido dentro de la partición.
     */
    public void publishOrderEvent(OrderEventDto event) {
        String key = "order-" + event.getOrderId();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] Error al publicar OrderEvent id={}: {}", event.getOrderId(), ex.getMessage());
            } else {
                log.info("[Kafka] OrderEvent publicado — topic={} partition={} offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publica un evento de cambio en catálogo (producto creado, actualizado o eliminado).
     * La clave es el productId para garantizar orden por producto.
     */
    public void publishCatalogEvent(CatalogEventDto event) {
        String key = "product-" + event.getProductId();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(catalogEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] Error al publicar CatalogEvent id={}: {}", event.getProductId(), ex.getMessage());
            } else {
                log.info("[Kafka] CatalogEvent publicado — topic={} partition={} offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
