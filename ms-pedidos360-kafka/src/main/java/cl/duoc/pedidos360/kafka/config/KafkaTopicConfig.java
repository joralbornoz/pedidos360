package cl.duoc.pedidos360.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los tópicos de Pedidos360 con sus configuraciones de retención,
 * particiones y factor de replicación. Spring Kafka los crea automáticamente
 * al arrancar si no existen.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${pedidos360.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${pedidos360.kafka.topics.catalog-events}")
    private String catalogEventsTopic;

    /**
     * Tópico de eventos de pedidos.
     * - 3 particiones para permitir consumo paralelo
     * - 1 réplica (clúster local de 1 broker en dev; ajustar en prod)
     * - Retención 7 días (604800000 ms)
     * - cleanup.policy: delete (mensajes se eliminan al expirar)
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")        // 7 días
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    /**
     * Tópico de eventos de catálogo (productos).
     * - 3 particiones
     * - 1 réplica
     * - Retención 3 días (259200000 ms)
     * - cleanup.policy: delete
     */
    @Bean
    public NewTopic catalogEventsTopic() {
        return TopicBuilder.name(catalogEventsTopic)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "259200000")        // 3 días
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }
}
