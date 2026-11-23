package cl.duoc.pedidos360.kafka.consumer;

import cl.duoc.pedidos360.kafka.service.streaming.DeadLetterService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Monitorea los Dead Letter Topics de los dos flujos.
 * Lee los headers que agrega DeadLetterPublishingRecoverer (tópico, partición y offset
 * originales + mensaje de la excepción) y los registra para poder revisarlos.
 *
 * Consumer group: pedidos360-dlt-monitor
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final DeadLetterService deadLetterService;

    public DeadLetterConsumer(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @KafkaListener(
            id = "dlt-monitor-listener",
            topics = {"${pedidos360.kafka.topics.order-events-dlt}", "${pedidos360.kafka.topics.catalog-events-dlt}"},
            groupId = "${pedidos360.kafka.consumer-groups.dlt-monitor}",
            containerFactory = "deadLetterListenerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String topicOriginal = headerAsString(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        Integer particionOriginal = headerAsInt(record, KafkaHeaders.DLT_ORIGINAL_PARTITION);
        Long offsetOriginal = headerAsLong(record, KafkaHeaders.DLT_ORIGINAL_OFFSET);
        String error = headerAsString(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);

        log.error("[Kafka][DLT] Mensaje fallido de {} partition={} offset={} key={} error={}",
                topicOriginal, particionOriginal, offsetOriginal, record.key(), error);

        deadLetterService.register(new DeadLetterService.DeadLetter(
                record.topic(), topicOriginal, particionOriginal, offsetOriginal,
                error, record.key(), record.value(), Instant.now()));
    }

    private String headerAsString(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private Integer headerAsInt(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null || header.value().length < 4 ? null : ByteBuffer.wrap(header.value()).getInt();
    }

    private Long headerAsLong(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null || header.value().length < 8 ? null : ByteBuffer.wrap(header.value()).getLong();
    }
}