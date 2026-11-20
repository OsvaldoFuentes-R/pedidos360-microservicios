package cl.duoc.pedidos360.notificaciones.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Mensaje que llega por la cola pedidos.notificaciones (lo publica ms-pedidos).
 * Ejemplo: {"pedidoId":5,"clienteId":1,"clienteNombre":"Camila","clienteEmail":"c@x.cl",
 *           "total":549990.00,"mensaje":"Tu pedido #5 fue registrado."}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoNotificacion(
        Long pedidoId,
        Long clienteId,
        String clienteNombre,
        String clienteEmail,
        BigDecimal total,
        String mensaje) {
}
