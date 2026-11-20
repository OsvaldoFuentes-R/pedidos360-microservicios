package cl.duoc.pedidos360.pedidos.error;

/**
 * La solicitud no se puede cumplir (cliente o producto inexistente, stock insuficiente, datos
 * incompletos). Al lanzarse dentro del consumidor, el mensaje se rechaza y termina en la DLQ.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
