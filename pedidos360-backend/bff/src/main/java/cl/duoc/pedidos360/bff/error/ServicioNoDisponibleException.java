package cl.duoc.pedidos360.bff.error;

/** Un microservicio o RabbitMQ no respondió. Se traduce en HTTP 503. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
