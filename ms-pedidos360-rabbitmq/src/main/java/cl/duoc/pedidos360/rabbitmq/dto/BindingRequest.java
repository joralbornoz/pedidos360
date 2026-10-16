package cl.duoc.pedidos360.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;

public class BindingRequest {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    private String routingKey;

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
}