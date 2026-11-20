package cl.duoc.pedidos360.kafkaadmin.error;

public class KafkaNoDisponibleException extends RuntimeException {

    public KafkaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
