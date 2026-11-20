package cl.duoc.pedidos360.despacho.dto;

/**
 * Evento que ms-despacho publica en pedidos.eventos cuando cambia el estado de un ticket.
 * La clave del mensaje es el pedidoId, igual que el evento PEDIDO_REGISTRADO de ms-pedidos: todos los eventos
 * de un pedido van a la misma partición y se leen en orden (registrado, en ruta, entregado).
 */
public record TicketEventoKafka(
        String eventId,
        String tipo,
        Long pedidoId,
        String codigoTicket,
        String estadoAnterior,
        String estadoNuevo,
        String fecha) {

    public static final String TICKET_ESTADO_CAMBIADO = "TICKET_ESTADO_CAMBIADO";
}
