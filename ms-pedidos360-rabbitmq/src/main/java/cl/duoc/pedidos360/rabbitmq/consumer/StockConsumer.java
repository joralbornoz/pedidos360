package cl.duoc.pedidos360.rabbitmq.consumer;

import cl.duoc.pedidos360.rabbitmq.config.RabbitMQConfig;
import cl.duoc.pedidos360.rabbitmq.dto.StockDecrementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Caso de uso 2: Descuento de stock al aceptar pedido.
 *
 * Consume mensajes de q.pedidos360.stock (routing: order.stock.decrement)
 * y llama al endpoint existente de ms-pedidos360-catalog:
 *   PATCH /api/catalog/{id}/stock?cantidad={n}
 */
@Component
public class StockConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockConsumer.class);

    private final RestTemplate restTemplate;
    private final String catalogUrl;

    public StockConsumer(@Value("${catalog.service.url}") String catalogUrl) {
        this.restTemplate = new RestTemplate();
        this.catalogUrl   = catalogUrl;
    }

    @RabbitListener(queues = RabbitMQConfig.Q_STOCK)
    public void handleStockDecrement(StockDecrementEvent event) {
        log.info("[STOCK] Solicitud descuento — pedido #{} | producto #{} | cantidad: {}",
                event.orderId(), event.productId(), event.cantidad());

        try {
            // Construye: PATCH <catalogUrl>/api/catalog/{productId}/stock?cantidad={n}
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(catalogUrl)
                    .path("/api/catalog/{id}/stock")
                    .queryParam("cantidad", event.cantidad())
                    .buildAndExpand(event.productId())
                    .toUri();

            ResponseEntity<String> response =
                    restTemplate.exchange(uri, HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

            log.info("[STOCK] Descuento aplicado OK — producto #{}, {} unidades. HTTP {}",
                    event.productId(), event.cantidad(), response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("[STOCK] Error HTTP al descontar stock — producto #{}: {} — {}",
                    event.productId(), e.getStatusCode(), e.getResponseBodyAsString());
            // Lanza excepción para que el mensaje se reencole y llegue a DLQ tras 3 reintentos
            throw e;
        } catch (Exception e) {
            log.error("[STOCK] Error inesperado — producto #{}: {}", event.productId(), e.getMessage());
            throw e;
        }
    }
}
