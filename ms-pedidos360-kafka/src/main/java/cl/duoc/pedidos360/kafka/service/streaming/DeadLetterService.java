package cl.duoc.pedidos360.kafka.service.streaming;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Guarda los últimos mensajes que terminaron en un DLT, con el motivo del error,
 * para poder revisarlos desde la API sin entrar a Kafka UI.
 */
@Service
public class DeadLetterService {

    private static final int MAX = 50;

    public record DeadLetter(String dltTopic, String topicOriginal, Integer particionOriginal,
                             Long offsetOriginal, String error, String key, String payload, Instant recibido) {
    }

    private final Deque<DeadLetter> mensajes = new ArrayDeque<>();

    public synchronized void register(DeadLetter deadLetter) {
        mensajes.addFirst(deadLetter);
        if (mensajes.size() > MAX) {
            mensajes.removeLast();
        }
    }

    public synchronized List<DeadLetter> recent() {
        return new ArrayList<>(mensajes);
    }
}