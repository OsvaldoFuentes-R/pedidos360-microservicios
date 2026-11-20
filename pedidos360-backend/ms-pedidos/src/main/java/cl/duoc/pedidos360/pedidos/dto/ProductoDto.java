package cl.duoc.pedidos360.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Lo que ms-pedidos necesita saber de un producto (respuesta de ms-productos). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(Long id, String nombre, BigDecimal precio, int stock) {
}
