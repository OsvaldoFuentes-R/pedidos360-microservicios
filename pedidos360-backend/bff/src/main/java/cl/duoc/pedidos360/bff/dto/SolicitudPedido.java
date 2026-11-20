package cl.duoc.pedidos360.bff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Cuerpo de POST /api/pedidos. Ejemplo: {"clienteId":1,"items":[{"productoId":1,"cantidad":2}]} */
public record SolicitudPedido(
        @NotNull(message = "clienteId es obligatorio") Long clienteId,
        @NotEmpty(message = "el pedido necesita al menos un item") List<@Valid ItemSolicitud> items) {

    public record ItemSolicitud(
            @NotNull(message = "productoId es obligatorio") Long productoId,
            @Min(value = 1, message = "la cantidad debe ser al menos 1") int cantidad) {
    }
}
