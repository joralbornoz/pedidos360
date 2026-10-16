package cl.duoc.pedidos360.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ExchangeRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String name;

    @NotBlank(message = "El tipo no puede estar vacío")
    @Pattern(regexp = "^(direct|topic|fanout|headers)$", message = "El tipo debe ser: direct, topic, fanout o headers")
    private String type = "topic";

    private boolean durable = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
}