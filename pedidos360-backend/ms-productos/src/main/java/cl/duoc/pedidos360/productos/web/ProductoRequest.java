package cl.duoc.pedidos360.productos.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        String descripcion,
        @NotNull(message = "el precio es obligatorio")
        @DecimalMin(value = "0.0", message = "el precio no puede ser negativo") BigDecimal precio,
        @Min(value = 0, message = "el stock no puede ser negativo") int stock,
        String categoria) {
}
