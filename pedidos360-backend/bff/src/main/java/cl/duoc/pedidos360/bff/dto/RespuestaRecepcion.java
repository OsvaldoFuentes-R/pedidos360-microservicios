package cl.duoc.pedidos360.bff.dto;

/** Respuesta 202: el pedido quedó en la cola y se procesará de forma asíncrona. */
public record RespuestaRecepcion(String estado, String mensaje) {
}
