package cl.duoc.pedidos360.kafka.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

/**
 * Convierte el JSON recibido desde Kafka al DTO indicado
 * Cada tópico tiene su propio deserializador con su tipo
 */
public class JsonEventDeserializer<T> implements Deserializer<T> {

    private final Class<T> targetType;
    private final ObjectMapper objectMapper;

    public JsonEventDeserializer(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = EventObjectMapper.create();
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, targetType);
        } catch (IOException e) {
            throw new SerializationException(
                    "JSON inválido en el tópico " + topic + " para " + targetType.getSimpleName(), e);
        }
    }
}