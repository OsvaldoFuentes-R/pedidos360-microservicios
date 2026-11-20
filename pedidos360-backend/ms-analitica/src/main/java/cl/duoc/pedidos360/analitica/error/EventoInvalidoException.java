package cl.duoc.pedidos360.analitica.error;

/** El mensaje no es un JSON válido o le faltan campos obligatorios. No se reintenta: va directo a la DLT. */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public EventoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
