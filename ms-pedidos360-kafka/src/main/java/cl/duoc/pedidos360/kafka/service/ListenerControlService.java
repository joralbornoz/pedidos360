package cl.duoc.pedidos360.kafka.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Permite detener e iniciar los @KafkaListener del microservicio en caliente.
 * Se usa para operaciones de mantención, resetear sus offsets y volver a iniciarlo para reprocesar los eventos.
 */
@Service
public class ListenerControlService {

    private static final Logger log = LoggerFactory.getLogger(ListenerControlService.class);

    private final KafkaListenerEndpointRegistry registry;

    public ListenerControlService(KafkaListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    public List<Map<String, Object>> listListeners() {
        return registry.getListenerContainerIds().stream()
                .sorted()
                .map(id -> {
                    MessageListenerContainer container = registry.getListenerContainer(id);
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("listenerId", id);
                    info.put("groupId", container.getGroupId());
                    info.put("activo", container.isRunning());
                    return info;
                })
                .toList();
    }

    public Map<String, Object> stop(String listenerId) {
        MessageListenerContainer container = find(listenerId);
        container.stop();
        log.info("[KafkaAdmin] Listener {} detenido (grupo {})", listenerId, container.getGroupId());
        return Map.of("listenerId", listenerId, "activo", container.isRunning());
    }

    public Map<String, Object> start(String listenerId) {
        MessageListenerContainer container = find(listenerId);
        container.start();
        log.info("[KafkaAdmin] Listener {} iniciado (grupo {})", listenerId, container.getGroupId());
        return Map.of("listenerId", listenerId, "activo", container.isRunning());
    }

    private MessageListenerContainer find(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            throw new IllegalArgumentException("No existe el listener " + listenerId
                    + ". Disponibles: " + registry.getListenerContainerIds());
        }
        return container;
    }
}