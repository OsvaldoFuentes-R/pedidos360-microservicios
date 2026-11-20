package cl.duoc.pedidos360.rabbitadmin.error;

/** El elemento ya existe con otra configuración (RabbitMQ responde PRECONDITION_FAILED). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    public ConflictoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
