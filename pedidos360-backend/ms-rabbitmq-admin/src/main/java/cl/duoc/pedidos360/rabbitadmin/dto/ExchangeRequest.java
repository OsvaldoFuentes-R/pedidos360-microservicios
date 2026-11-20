package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Ejemplo: {"nombre":"pedidos360.auditoria","tipo":"TOPIC","durable":true} */
public record ExchangeRequest(
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @NotNull(message = "el tipo es obligatorio (DIRECT, TOPIC, FANOUT o HEADERS)") TipoExchange tipo,
        Boolean durable,
        String alternateExchange) {

    public boolean esDurable() {
        return durable == null || durable;
    }
}
