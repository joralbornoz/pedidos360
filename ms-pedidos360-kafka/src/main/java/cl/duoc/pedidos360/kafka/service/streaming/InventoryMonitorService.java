package cl.duoc.pedidos360.kafka.service.streaming;

import cl.duoc.pedidos360.kafka.dto.CatalogEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica de negocio: monitor de inventario en tiempo real.
 * Mantiene el stock actual de cada producto y genera una alerta automática
 * cuando el stock baja del umbral configurado.
 */
@Service
public class InventoryMonitorService {

    private static final Logger log = LoggerFactory.getLogger(InventoryMonitorService.class);

    public record ProductState(Long productId, String nombre, Double precio, Integer stock, Instant actualizado) {
    }

    private final int umbralStockBajo;
    private final Map<Long, ProductState> productos = new ConcurrentHashMap<>();
    private final Map<Long, String> alertas = new ConcurrentHashMap<>();

    public InventoryMonitorService(@Value("${pedidos360.inventory.low-stock-threshold}") int umbralStockBajo) {
        this.umbralStockBajo = umbralStockBajo;
    }

    public void process(CatalogEventDto event) {
        Long id = event.getProductId();

        if ("PRODUCT_DELETED".equals(event.getEventType())) {
            productos.remove(id);
            alertas.remove(id);
            log.info("[Inventario] Producto #{} eliminado del monitor", id);
            return;
        }

        productos.put(id, new ProductState(id, event.getNombre(), event.getPrecio(),
                event.getStock(), event.getTimestamp()));

        Integer stock = event.getStock();
        if (stock != null && stock <= umbralStockBajo) {
            String alerta = String.format("Stock bajo: %s (#%d) quedan %d unidades", event.getNombre(), id, stock);
            alertas.put(id, alerta);
            log.warn("[Inventario] ALERTA {}", alerta);
        } else {
            alertas.remove(id);
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("umbralStockBajo", umbralStockBajo);
        result.put("productosMonitoreados", productos.size());
        result.put("productos", new TreeMap<>(productos));
        result.put("alertas", new TreeMap<>(alertas));
        return result;
    }
}