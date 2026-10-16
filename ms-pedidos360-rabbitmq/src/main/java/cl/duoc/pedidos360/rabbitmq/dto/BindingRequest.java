package cl.duoc.pedidos360.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BindingRequest {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(max = 100, message = "El nombre de la cola no puede superar los 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(max = 100, message = "El nombre del exchange no puede superar los 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    @Size(max = 255, message = "La routing key no puede superar los 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._*#-]+$", message = "La routing key solo puede contener letras, números, puntos, guiones, guiones bajos y los comodines * y #")
    private String routingKey;

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
}