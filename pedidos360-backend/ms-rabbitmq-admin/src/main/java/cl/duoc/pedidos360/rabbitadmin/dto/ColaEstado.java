package cl.duoc.pedidos360.rabbitadmin.dto;

/** Estado en vivo de una cola (consultado por AMQP). */
public record ColaEstado(String nombre, int mensajes, int consumidores) {
}
