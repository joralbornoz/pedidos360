package cl.duoc.pedidos360.kafka.exception;

import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Traduce los errores del AdminClient de Kafka a respuestas HTTP claras
 * (409 si el tópico ya existe, 404 si no existe, 400 si la config es inválida, etc.).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleKafkaError(ExecutionException ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        HttpStatus status = switch (cause) {
            case TopicExistsException e -> HttpStatus.CONFLICT;
            case GroupNotEmptyException e -> HttpStatus.CONFLICT;
            case UnknownTopicOrPartitionException e -> HttpStatus.NOT_FOUND;
            case GroupIdNotFoundException e -> HttpStatus.NOT_FOUND;
            case InvalidPartitionsException e -> HttpStatus.BAD_REQUEST;
            case InvalidReplicationFactorException e -> HttpStatus.BAD_REQUEST;
            case InvalidConfigurationException e -> HttpStatus.BAD_REQUEST;
            case InvalidRequestException e -> HttpStatus.BAD_REQUEST;
            case PolicyViolationException e -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_GATEWAY;
        };
        log.warn("[KafkaAdmin] Error de Kafka ({}): {}", status.value(), cause.getMessage());
        return body(status, cause.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(TimeoutException ex) {
        return body(HttpStatus.GATEWAY_TIMEOUT, "Kafka no respondió a tiempo. ¿Están arriba los brokers?");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return body(HttpStatus.BAD_REQUEST, detail);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", message);
        body.put("timestamp", Instant.now());
        return ResponseEntity.status(status).body(body);
    }
}