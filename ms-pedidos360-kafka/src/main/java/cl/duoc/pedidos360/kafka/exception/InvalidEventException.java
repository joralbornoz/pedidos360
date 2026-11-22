package cl.duoc.pedidos360.kafka.exception;

/**
 * El evento llegó bien formado (JSON válido) pero con datos que no cumplen el contrato:
 * campos obligatorios vacíos, valores negativos, etc.
 * Reintentarlo no sirve de nada, así que el error handler lo manda directo al DLT.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}