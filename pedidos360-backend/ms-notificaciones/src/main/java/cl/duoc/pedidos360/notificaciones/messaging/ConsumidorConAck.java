package cl.duoc.pedidos360.notificaciones.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Base para consumidores con confirmación manual (ACK/NACK).
 *
 * <ul>
 *   <li>Si el procesamiento termina bien: ACK, el mensaje sale de la cola.</li>
 *   <li>Si lanza cualquier excepción: NACK sin reencolar, y la cola (que apunta a un DLX)
 *       lo envía a su DLQ para revisarlo.</li>
 * </ul>
 */
public abstract class ConsumidorConAck {

    /** Lógica de negocio que procesa el cuerpo del mensaje; cualquier excepción lo manda a la DLQ. */
    @FunctionalInterface
    public interface Procesador {
        void ejecutar(String mensaje) throws Exception;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String cola;

    protected ConsumidorConAck(String cola) {
        this.cola = cola;
    }

    protected void procesar(String mensaje, Channel channel, long deliveryTag, Procesador procesador)
            throws IOException {
        try {
            procesador.ejecutar(mensaje);
        } catch (Exception e) {
            log.warn("[{}] Mensaje FALLIDO -> se envía a la DLQ (NACK): '{}' | motivo: {}",
                    cola, mensaje, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        channel.basicAck(deliveryTag, false);
        log.info("[{}] Mensaje procesado (ACK): {}", cola, mensaje);
    }
}
