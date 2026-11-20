package cl.duoc.pedidos360.bff.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ticket tal como lo devuelve ms-despacho. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketRemoto(Long id, Long pedidoId, String codigo, String estado) {
}
