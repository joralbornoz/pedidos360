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
 *
 * Es idempotente: si un evento llega dos veces (reentrega o reset de offsets)
 * no se agrega repetido al historial.
 */
@Service
public class OrderTrackingService {

    public record Transition(String eventId, String eventType, String estadoAnterior,
                             String estadoNuevo, Instant timestamp) {
    }

    private final Map<Long, List<Transition>> timelines = new ConcurrentHashMap<>();

    public synchronized void process(OrderEventDto event) {
        List<Transition> timeline = timelines.computeIfAbsent(event.getOrderId(), id -> new ArrayList<>());
        boolean yaRegistrado = event.getEventId() != null
                && timeline.stream().anyMatch(t -> event.getEventId().equals(t.eventId()));
        if (yaRegistrado) {
            return;
        }
        timeline.add(new Transition(event.getEventId(), event.getEventType(), event.getEstadoAnterior(),
                event.getEstadoNuevo(), event.getTimestamp()));
    }

    public synchronized List<Transition> timeline(Long orderId) {
        return List.copyOf(timelines.getOrDefault(orderId, List.of()));
    }

    public int pedidosConHistorial() {
        return timelines.size();
    }
}