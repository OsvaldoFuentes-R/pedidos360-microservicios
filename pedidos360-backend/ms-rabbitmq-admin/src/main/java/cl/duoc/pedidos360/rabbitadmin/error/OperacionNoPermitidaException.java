package cl.duoc.pedidos360.rabbitadmin.error;

/** Operación bloqueada por una regla del servicio (elementos protegidos de la arquitectura). */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }

    public OperacionNoPermitidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
