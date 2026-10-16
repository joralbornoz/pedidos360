
package cl.duoc.pedidos360.rabbitmq.consumer.support;
 
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
 
import java.io.IOException;
 
/**
 * Política única de confirmación de mensajes (ACK/NACK) para todos los consumidores.
 * Los tres consumidores usan ackMode MANUAL y
 * delegan aquí la decisión, para que el criterio sea el mismo en todo el servicio:
 * 
 *   1. Éxito                -> ACK. El mensaje se elimina de la cola.
 *   2. Error NO recuperable -> NACK sin requeue. El mensaje va directo a la DLQ de
 *                              su cola (reintentar daría el mismo error).
 *   3. Error recuperable    -> NACK con requeue, una sola vez. Si el mensaje vuelve
 *                              a fallar (llega con redelivered = true), NACK sin
 *                              requeue y va a la DLQ. Así no hay bucles infinitos.
 *
 * Errores NO recuperables: datos inválidos, respuestas 4xx del catálogo (HttpClientErrorException) y violaciones de integridad en la base.
 * Cualquier otro error se considera recuperable.
 */
@Component
public class MessageAck {
 
    private static final Logger log = LoggerFactory.getLogger(MessageAck.class);
 
    /** Confirma el mensaje: se procesó bien y sale de la cola. */
    public void ack(Channel channel, long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }
 
    /**
     * Decide qué hacer con un mensaje que falló.
     *
     * @param redelivered true si RabbitMQ ya había intentado entregar este mensaje
     * @param consumer    nombre del consumidor, solo para identificarlo en los logs
     */
    public void reject(Channel channel, long deliveryTag, boolean redelivered,
                       Exception error, String consumer) throws IOException {
 
        if (!isRecoverable(error)) {
            log.error("[{}] Error no recuperable, enviando a DLQ: {}", consumer, error.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } else if (!redelivered) {
            log.warn("[{}] Error recuperable, se reintenta una vez mas: {}", consumer, error.getMessage());
            channel.basicNack(deliveryTag, false, true);
        } else {
            log.error("[{}] Reintento fallido, enviando a DLQ: {}", consumer, error.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
 
    /** Un error es recuperable si reintentar tiene sentido (falla temporal, no dato malo). */
    private boolean isRecoverable(Exception e) {
        return !(e instanceof IllegalArgumentException
                || e instanceof HttpClientErrorException
                || e instanceof DataIntegrityViolationException);
    }
}