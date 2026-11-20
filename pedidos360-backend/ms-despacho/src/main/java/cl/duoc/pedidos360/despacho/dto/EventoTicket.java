package cl.duoc.pedidos360.despacho.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mensaje que llega por la cola pedidos.tickets (lo publica ms-pedidos).
 * Ejemplo: {"pedidoId":5,"clienteNombre":"Camila","direccionEntrega":"Av. 1","totalItems":3}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoTicket(
        Long pedidoId,
        String clienteNombre,
        String direccionEntrega,
        int totalItems) {
}
