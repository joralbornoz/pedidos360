package cl.duoc.pedidos360.rabbitmq.consumer.support;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
@Component
public class MessageAck {

    private static final Logger log = LoggerFactory.getLogger(MessageAck.class);

    public void ack(Channel channel, long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

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

    private boolean isRecoverable(Exception e) {
        return !(e instanceof IllegalArgumentException
                || e instanceof HttpClientErrorException
                || e instanceof DataIntegrityViolationException);
    }
}