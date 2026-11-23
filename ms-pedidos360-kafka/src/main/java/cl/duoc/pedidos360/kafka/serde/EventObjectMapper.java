package cl.duoc.pedidos360.kafka.serde;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * ObjectMapper compartido por el serializador y el deserializador de eventos
 * - Fechas en formato ISO (2026-11-22T13:50:00Z) y no como números
 * - Ignora campos desconocidos: si un productor agrega un campo nuevo,
 *   los consumidores antiguos no se caen
 */
public final class EventObjectMapper {

    private EventObjectMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}