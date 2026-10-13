package cl.duoc.pedidos360.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ExchangeRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
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