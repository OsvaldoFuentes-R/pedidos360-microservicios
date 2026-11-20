package cl.duoc.pedidos360.productos.web;

import jakarta.validation.constraints.Min;

public record CantidadRequest(
        @Min(value = 1, message = "la cantidad debe ser al menos 1") int cantidad) {
}
