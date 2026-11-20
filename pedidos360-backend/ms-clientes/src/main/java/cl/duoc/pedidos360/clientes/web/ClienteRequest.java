package cl.duoc.pedidos360.clientes.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido") String email,
        String telefono,
        @NotBlank(message = "la dirección es obligatoria") String direccion) {
}
