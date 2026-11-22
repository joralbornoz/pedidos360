package cl.duoc.pedidos360.catalog.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Convierte un DTO a JSON (bytes) antes de enviarlo a Kafka.
 * No agrega el header __TypeId__ con el nombre de la clase, así el consumidor
 * no depende del paquete de este microservicio.
 */
public class JsonEventSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper;

    public JsonEventSerializer() {
        this.objectMapper = EventObjectMapper.create();
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("No se pudo serializar el evento para el tópico " + topic, e);
        }
    }
}