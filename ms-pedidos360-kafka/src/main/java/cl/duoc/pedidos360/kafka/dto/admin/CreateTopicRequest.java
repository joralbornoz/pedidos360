package cl.duoc.pedidos360.kafka.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * Datos para crear un tópico desde la API de administración.
 * configs es opcional, ej: {"retention.ms": "86400000", "cleanup.policy": "delete"}
 */
public record CreateTopicRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "solo letras, números, punto, guion y guion bajo")
        String name,

        @NotNull @Min(1) @Max(12)
        Integer partitions,

        @NotNull @Min(1) @Max(3)
        Integer replicationFactor,

        Map<String, String> configs) {
}