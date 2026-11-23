package cl.duoc.pedidos360.kafka.service.streaming;

import cl.duoc.pedidos360.kafka.dto.OrderEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lógica de negocio: KPIs de pedidos calculados en tiempo real a partir del stream.
 * No sabe nada de Kafka: solo recibe un OrderEventDto y actualiza las métricas.
 *
 * Se usa synchronized porque el consumidor corre con 3 hilos (uno por partición).
 */
@Service
public class OrderAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(OrderAnalyticsService.class);
    private static final int MAX_EVENT_IDS = 5000;
    private static final int MAX_RECENT_EVENTS = 20;

    private long totalEventos;
    private long pedidosCreados;
    private long cambiosDeEstado;
    private long pedidosEliminados;
    private long duplicadosIgnorados;
    private Instant ultimoEvento;

    /** Cantidad actual de pedidos en cada estado (PENDIENTE, PAGADO, ENVIADO...). */
    private final Map<String, Long> pedidosPorEstado = new TreeMap<>();

    /** Últimos eventos procesados, para mostrarlos en la demo. */
    private final Deque<String> eventosRecientes = new ArrayDeque<>();

    /** eventIds ya procesados: si Kafka reentrega un mensaje, no se cuenta dos veces. */
    private final Set<String> eventIdsProcesados = Collections.newSetFromMap(
            new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_EVENT_IDS;
                }
            });

    public synchronized void process(OrderEventDto event) {
        if (event.getEventId() != null && !eventIdsProcesados.add(event.getEventId())) {
            duplicadosIgnorados++;
            log.warn("[Analytics] Evento duplicado ignorado eventId={}", event.getEventId());
            return;
        }

        totalEventos++;
        ultimoEvento = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();

        switch (event.getEventType()) {
            case "ORDER_CREATED" -> {
                pedidosCreados++;
                sumar(event.getEstadoNuevo(), 1);
            }
            case "ORDER_STATUS_CHANGED" -> {
                cambiosDeEstado++;
                sumar(event.getEstadoAnterior(), -1);
                sumar(event.getEstadoNuevo(), 1);
            }
            case "ORDER_DELETED" -> {
                pedidosEliminados++;
                sumar(event.getEstadoAnterior(), -1);
            }
            default -> log.warn("[Analytics] eventType desconocido: {}", event.getEventType());
        }

        registrarReciente(String.format("%s pedido #%d (%s → %s)",
                event.getEventType(), event.getOrderId(),
                event.getEstadoAnterior(), event.getEstadoNuevo()));
    }

    /** Foto actual de los KPIs. */
    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalEventos", totalEventos);
        kpis.put("pedidosCreados", pedidosCreados);
        kpis.put("cambiosDeEstado", cambiosDeEstado);
        kpis.put("pedidosEliminados", pedidosEliminados);
        kpis.put("duplicadosIgnorados", duplicadosIgnorados);
        kpis.put("pedidosPorEstado", new TreeMap<>(pedidosPorEstado));
        kpis.put("ultimoEvento", ultimoEvento);
        kpis.put("eventosRecientes", new ArrayList<>(eventosRecientes));
        return kpis;
    }

    private void sumar(String estado, long delta) {
        if (estado == null || estado.isBlank()) {
            return;
        }
        long nuevo = pedidosPorEstado.getOrDefault(estado, 0L) + delta;
        if (nuevo <= 0) {
            pedidosPorEstado.remove(estado);
        } else {
            pedidosPorEstado.put(estado, nuevo);
        }
    }

    private void registrarReciente(String texto) {
        eventosRecientes.addFirst(texto);
        if (eventosRecientes.size() > MAX_RECENT_EVENTS) {
            eventosRecientes.removeLast();
        }
    }
}