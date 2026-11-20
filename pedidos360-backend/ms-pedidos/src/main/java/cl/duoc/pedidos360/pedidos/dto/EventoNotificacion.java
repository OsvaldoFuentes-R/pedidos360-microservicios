package cl.duoc.pedidos360.pedidos.dto;

import java.math.BigDecimal;

/** Evento que se publica en pedidos.notificaciones cuando un pedido queda registrado. */
public record EventoNotificacion(
        Long pedidoId,
        Long clienteId,
        String clienteNombre,
        String clienteEmail,
        BigDecimal total,
        String mensaje) {
}
