package cl.duoc.pedidos360.rabbitmq.consumer;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import cl.duoc.pedidos360.rabbitmq.consumer.support.MessageAck;
import cl.duoc.pedidos360.rabbitmq.dto.StockDecrementEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Component
public class StockConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockConsumer.class);

    private final RestTemplate restTemplate;
    private final String catalogUrl;
    private final MessageAck acknowledger;

    public StockConsumer(@Value("${catalog.service.url}") String catalogUrl,
                         MessageAck acknowledger) {
        this.restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());
        this.catalogUrl   = catalogUrl;
        this.acknowledger = acknowledger;
    }

    @RabbitListener(queues = "${pedidos360.rabbit.queues.stock}", ackMode = "MANUAL")
    public void handleStockDecrement(StockDecrementEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                     @Header(AmqpHeaders.REDELIVERED) boolean redelivered)
            throws IOException {
        try {
            if (event.productId() == null || event.cantidad() == null || event.cantidad() <= 0) {
                throw new IllegalArgumentException("Evento de stock sin productId o con cantidad inválida");
            }

            log.info("[STOCK] Solicitud descuento — pedido #{} | producto #{} | cantidad: {}",
                    event.orderId(), event.productId(), event.cantidad());

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

            acknowledger.ack(channel, deliveryTag);

        } catch (Exception e) {
            acknowledger.reject(channel, deliveryTag, redelivered, e, "STOCK");
        }
    }
}