package cl.duoc.pedidos360.productos.dto;

/**
 * Evento que ms-productos publica en inventario.movimientos cada vez que cambia el stock por un pedido.
 * La clave del mensaje es el productoId, así los movimientos de un producto conservan su orden.
 */
public record MovimientoInventarioKafka(
        String eventId,
        String tipo,
        Long productoId,
        String nombreProducto,
        int cantidad,
        int stockResultante,
        String fecha) {

    public static final String RESERVA = "RESERVA";
    public static final String LIBERACION = "LIBERACION";
}
