package cl.duoc.pedidos360.pedidos.error;

/** Un microservicio del que depende el pedido no respondió (caído, lento o con error). */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
