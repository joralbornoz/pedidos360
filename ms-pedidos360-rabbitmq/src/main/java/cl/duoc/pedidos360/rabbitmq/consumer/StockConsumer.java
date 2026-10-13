package cl.duoc.pedidos360.rabbitmq.consumer;

import cl.duoc.pedidos360.rabbitmq.config.RabbitMQConfig;
import cl.duoc.pedidos360.rabbitmq.dto.StockDecrementEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Component
public class StockConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockConsumer.class);

    private final RestTemplate restTemplate;
    private final String catalogUrl;

    public StockConsumer(@Value("${catalog.service.url}") String catalogUrl) {
        this.restTemplate = new RestTemplate();
        this.catalogUrl   = catalogUrl;
    }

    @RabbitListener(queues = RabbitMQConfig.Q_STOCK, ackMode = "MANUAL")
    public void handleStockDecrement(StockDecrementEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {

        log.info("[STOCK] Solicitud descuento — pedido #{} | producto #{} | cantidad: {}",
                event.orderId(), event.productId(), event.cantidad());

        try {
            URI uri = UriComponentsBuilder
                    .fromUri(URI.create(catalogUrl))
                    .path("/api/catalog/{id}/stock")
                    .queryParam("cantidad", event.cantidad())
                    .buildAndExpand(event.productId())
                    .toUri();

            ResponseEntity<String> response =
                    restTemplate.exchange(uri, HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

            log.info("[STOCK] Descuento aplicado OK — producto #{}, {} unidades. HTTP {}",
                    event.productId(), event.cantidad(), response.getStatusCode());

            channel.basicAck(deliveryTag, false);

        } catch (HttpClientErrorException e) {
            log.error("[STOCK] Error HTTP al descontar stock — producto #{}: {} — {}. Enviando a DLQ.",
                    event.productId(), e.getStatusCode(), e.getResponseBodyAsString());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[STOCK] Error inesperado — producto #{}: {}. Reencolar para reintento.",
                    event.productId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }
}