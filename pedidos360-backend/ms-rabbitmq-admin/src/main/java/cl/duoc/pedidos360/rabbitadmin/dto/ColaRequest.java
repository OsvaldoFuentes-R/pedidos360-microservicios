package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Ejemplo con DLQ:
 * {"nombre":"pedidos.auditoria","durable":true,"deadLetterExchange":"pedidos360.dlx",
 *  "deadLetterRoutingKey":"dlq.pedido.auditoria","ttlMs":60000}
 */
public record ColaRequest(
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        Boolean durable,
        String deadLetterExchange,
        String deadLetterRoutingKey,
        @Min(value = 1, message = "ttlMs debe ser mayor que cero") Long ttlMs) {

    public boolean esDurable() {
        return durable == null || durable;
    }
}
