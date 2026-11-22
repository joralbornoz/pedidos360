package cl.duoc.pedidos360.kafka.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Reset de offsets de un consumer group sobre un tópico.
 * strategy: earliest (volver al inicio y reprocesar todo), latest (saltar al final)
 *           u offset (ir a un offset específico en todas las particiones).
 */
public record ResetOffsetsRequest(
        @NotBlank
        String topic,

        @NotBlank
        @Pattern(regexp = "earliest|latest|offset", message = "debe ser earliest, latest u offset")
        String strategy,

        @PositiveOrZero
        Long offset) {
}