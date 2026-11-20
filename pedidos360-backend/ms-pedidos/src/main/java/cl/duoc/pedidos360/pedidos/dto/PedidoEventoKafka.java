package cl.duoc.pedidos360.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento que ms-pedidos publica en el tópico pedidos.eventos cuando un pedido queda registrado.
 * La clave del mensaje es el pedidoId: todos los eventos de un pedido van a la misma partición, en orden.
 */
public record PedidoEventoKafka(
        String eventId,
        String tipo,
        Long pedidoId,
        Long clienteId,
        String clienteNombre,
        BigDecimal total,
        String fecha,
        List<ItemEvento> items) {

    public static final String PEDIDO_REGISTRADO = "PEDIDO_REGISTRADO";

    public record ItemEvento(Long productoId, String nombreProducto, int cantidad, BigDecimal precioUnitario) {
    }
}
