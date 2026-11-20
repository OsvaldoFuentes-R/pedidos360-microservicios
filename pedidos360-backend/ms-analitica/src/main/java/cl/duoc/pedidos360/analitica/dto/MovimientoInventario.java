package cl.duoc.pedidos360.analitica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Evento del tópico inventario.movimientos (lo publica ms-productos). La clave del mensaje es el productoId. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovimientoInventario(
        String eventId,
        String tipo,
        Long productoId,
        String nombreProducto,
        int cantidad,
        int stockResultante,
        String fecha) {
}
