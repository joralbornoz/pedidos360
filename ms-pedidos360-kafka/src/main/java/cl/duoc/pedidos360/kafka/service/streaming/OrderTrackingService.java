package cl.duoc.pedidos360.kafka.service.streaming;

import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica de negocio: historial (línea de tiempo) de cada pedido.
 * Consume el mismo tópico que OrderAnalyticsService pero con OTRO consumer group,
 * así los dos reciben todos los eventos de forma independiente.
 */
@Service
public class OrderTrackingService {

    public record Transition(String eventType, String estadoAnterior, String estadoNuevo, Instant timestamp) {
    }

    private final Map<Long, List<Transition>> timelines = new ConcurrentHashMap<>();

    public void process(OrderEventDto event) {
        timelines.computeIfAbsent(event.getOrderId(), id -> new ArrayList<>())
                .add(new Transition(event.getEventType(), event.getEstadoAnterior(),
                        event.getEstadoNuevo(), event.getTimestamp()));
    }

    public List<Transition> timeline(Long orderId) {
        return List.copyOf(timelines.getOrDefault(orderId, List.of()));
    }

    public int pedidosConHistorial() {
        return timelines.size();
    }
}