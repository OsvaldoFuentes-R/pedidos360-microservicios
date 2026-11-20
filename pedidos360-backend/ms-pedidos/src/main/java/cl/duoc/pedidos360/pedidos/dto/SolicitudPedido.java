package cl.duoc.pedidos360.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mensaje que llega por la cola pedidos.registro (lo publica el BFF).
 * Ejemplo: {"clienteId":1,"items":[{"productoId":1,"cantidad":2}]}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudPedido(Long clienteId, List<ItemSolicitud> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemSolicitud(Long productoId, int cantidad) {
    }
}
