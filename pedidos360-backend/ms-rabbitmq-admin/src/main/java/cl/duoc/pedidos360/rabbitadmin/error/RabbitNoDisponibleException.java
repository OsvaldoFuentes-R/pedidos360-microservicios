package cl.duoc.pedidos360.rabbitadmin.error;

/** RabbitMQ no respondió. */
public class RabbitNoDisponibleException extends RuntimeException {

    public RabbitNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public RabbitNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
