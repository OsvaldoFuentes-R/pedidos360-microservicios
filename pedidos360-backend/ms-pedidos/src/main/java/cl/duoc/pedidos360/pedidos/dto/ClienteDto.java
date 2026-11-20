package cl.duoc.pedidos360.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Lo que ms-pedidos necesita saber de un cliente (respuesta de ms-clientes). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteDto(Long id, String nombre, String email, String direccion) {
}
