package cl.duoc.pedidos360.kafkaadmin.error;

/** Operación bloqueada por una regla del servicio (por ejemplo, borrar un tópico protegido). */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
