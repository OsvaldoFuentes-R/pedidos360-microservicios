package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Ejemplo: {"exchange":"pedidos360.auditoria","destino":"pedidos.auditoria","tipoDestino":"QUEUE","routingKey":"pedido.#"} */
public record BindingRequest(
        @NotBlank(message = "el exchange de origen es obligatorio") String exchange,
        @NotBlank(message = "el destino es obligatorio") String destino,
        @NotNull(message = "tipoDestino es obligatorio (QUEUE o EXCHANGE)") TipoDestino tipoDestino,
        String routingKey) {

    public String clave() {
        return routingKey == null ? "" : routingKey;
    }
}
