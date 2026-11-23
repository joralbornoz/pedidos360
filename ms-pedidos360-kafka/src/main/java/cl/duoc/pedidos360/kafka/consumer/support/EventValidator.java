package cl.duoc.pedidos360.kafka.consumer.support;

import cl.duoc.pedidos360.kafka.exception.InvalidEventException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida el payload de cada evento con las anotaciones del DTO (@NotNull, @NotBlank, @PositiveOrZero).
 * Si algo no cumple, lanza InvalidEventException y el mensaje se va al DLT sin reintentos.
 */
@Component
public class EventValidator {

    private final Validator validator;

    public EventValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T event) {
        if (event == null) {
            throw new InvalidEventException("El mensaje llegó sin contenido (value null)");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detalle = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidEventException(event.getClass().getSimpleName() + " inválido: " + detalle);
        }
        return event;
    }
}