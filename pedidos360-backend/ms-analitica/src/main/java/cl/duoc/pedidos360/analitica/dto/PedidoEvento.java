package cl.duoc.pedidos360.analitica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** Evento del tópico pedidos.eventos (lo publica ms-pedidos). La clave del mensaje es el pedidoId. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEvento(
        String eventId,
        String tipo,
        Long pedidoId,
        Long clienteId,
        String clienteNombre,
        BigDecimal total,
        String fecha,
        List<ItemEvento> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemEvento(Long productoId, String nombreProducto, int cantidad, BigDecimal precioUnitario) {
    }
}
