package cl.duoc.pedidos360.despacho.web;

import cl.duoc.pedidos360.despacho.model.EstadoTicket;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "el estado es obligatorio (GENERADO, EN_RUTA o ENTREGADO)") EstadoTicket estado) {
}
