package cl.duoc.pedidos360.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los tópicos de Pedidos360 con su configuración.
 * KafkaAdmin los crea al arrancar si no existen (si ya los creó el script
 * create-topics.sh del docker-compose, no hace nada).
 *
 * Todos usan 3 particiones (hasta 3 consumidores en paralelo por grupo)
 * y 3 réplicas con min.insync.replicas=2 (se puede caer 1 broker sin perder datos).
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${pedidos360.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${pedidos360.kafka.topics.catalog-events}")
    private String catalogEventsTopic;

    @Value("${pedidos360.kafka.topics.order-events-dlt}")
    private String orderEventsDltTopic;

    @Value("${pedidos360.kafka.topics.catalog-events-dlt}")
    private String catalogEventsDltTopic;

    @Value("${pedidos360.kafka.topic-defaults.partitions}")
    private int partitions;

    @Value("${pedidos360.kafka.topic-defaults.replicas}")
    private int replicas;

    @Value("${pedidos360.kafka.topic-defaults.min-insync-replicas}")
    private String minInsyncReplicas;

    /**
     * Eventos del ciclo de vida de los pedidos.
     * cleanup.policy=delete con 7 días: es un historial, sirve para re-procesar
     * o recalcular métricas de la última semana.
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")      // 7 días
                .config(TopicConfig.SEGMENT_MS_CONFIG, "86400000")         // segmentos de 1 día
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }

    /**
     * Cambios del catálogo (key = product-{id}).
     * cleanup.policy=compact: Kafka conserva el último evento de cada producto,
     * así el tópico funciona como el estado actual del catálogo.
     */
    @Bean
    public NewTopic catalogEventsTopic() {
        return TopicBuilder.name(catalogEventsTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "60000")  // espera 1 min antes de compactar
                .config(TopicConfig.SEGMENT_MS_CONFIG, "3600000")          // segmentos de 1 hora
                .config(TopicConfig.DELETE_RETENTION_MS_CONFIG, "86400000")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }

    @Bean
    public NewTopic orderEventsDltTopic() {
        return dltTopic(orderEventsDltTopic);
    }

    @Bean
    public NewTopic catalogEventsDltTopic() {
        return dltTopic(catalogEventsDltTopic);
    }

    private NewTopic dltTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "1209600000")     // 14 días
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }
}