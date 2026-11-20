package cl.duoc.pedidos360.pedidos.dto;

/** Evento que se publica en pedidos.tickets para que ms-despacho genere el ticket de despacho. */
public record EventoTicket(
        Long pedidoId,
        String clienteNombre,
        String direccionEntrega,
        int totalItems) {
}
