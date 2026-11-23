package cl.duoc.pedidos360.kafka.config;

import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import cl.duoc.pedidos360.kafka.exception.InvalidEventException;
import cl.duoc.pedidos360.kafka.serde.JsonEventDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de los consumidores Kafka.
 *
 * - Una ConsumerFactory y una ListenerContainerFactory por tópico, cada una con el deserializador de su DTO.
 * - ErrorHandlingDeserializer: si un mensaje no se puede leer (JSON malo) no bloquea la partición.
 * - DefaultErrorHandler: reintentos con backoff exponencial y, si se agotan, envío al DLT.
 * El group.id no va aquí: lo define cada @KafkaListener.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** Propiedades comunes a todos los consumidores. */
    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Un grupo nuevo parte leyendo desde el inicio del tópico
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // El commit del offset lo hace el contenedor de Spring, no el cliente automáticamente
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Máximo de mensajes por poll y tiempo máximo para procesarlos
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        // Si el consumidor no manda heartbeat en 15 s, el grupo lo saca y reparte sus particiones
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);
        // Rebalanceo incremental: al entrar o salir un consumidor no se detiene todo el grupo
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        // Solo lee mensajes confirmados
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        // Los tópicos solo se crean de forma controlada, nunca al suscribirse
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        return props;
    }

    // ── Manejo de errores ────────────────────────────────────────────────────

    /**
     * Error handler compartido por todos los listeners:
     * 1. Reintenta 3 veces con espera exponencial (1 s, 2 s, 4 s) → cubre fallas temporales.
     * 2. Si sigue fallando, publica el mensaje en <tópico>.DLT con headers que explican el error.
     * 3. Errores que no se arreglan reintentando (JSON malo, datos inválidos) van directo al DLT.
     * Así un mensaje defectuoso nunca detiene el consumo del resto.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setMaxAttempts(3);
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidEventException.class, IllegalArgumentException.class);
        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("[Kafka][Retry] Intento {} fallido topic={} partition={} offset={} error={}",
                        attempt, record.topic(), record.partition(), record.offset(), ex.getMessage()));
        return errorHandler;
    }

    // ── Tópico pedidos360.order-events ───────────────────────────────────────

    @Bean
    public ConsumerFactory<String, OrderEventDto> orderEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonEventDeserializer<>(OrderEventDto.class)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEventDto> orderEventListenerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, OrderEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    // ── Tópico pedidos360.catalog-events ─────────────────────────────────────

    @Bean
    public ConsumerFactory<String, CatalogEventDto> catalogEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonEventDeserializer<>(CatalogEventDto.class)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDto> catalogEventListenerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(catalogEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    // ── Dead Letter Topics (se leen como texto, para inspeccionar cualquier contenido) ──

    @Bean
    public ConsumerFactory<String, String> deadLetterConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(),
                new StringDeserializer(),
                new StringDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> deadLetterListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deadLetterConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}