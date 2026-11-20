package cl.duoc.pedidos360.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Nombres de la topología RabbitMQ, leídos de application.yml (prefijo "pedidos360.rabbitmq").
 * Los nombres viven en un solo lugar: ninguna clase Java escribe colas ni routing keys a mano.
 *
 * @param declarar nombres de las rutas (claves de "rutas") que este servicio declara en el broker
 */
@ConfigurationProperties(prefix = "pedidos360.rabbitmq")
public record RabbitMQProperties(
        String exchange,
        String dlx,
        String unroutableExchange,
        String unroutableQueue,
        List<String> declarar,
        Map<String, Route> rutas) {

    /** Un caso de uso de mensajería: cola principal, routing key y su cola de mensajes fallidos. */
    public record Route(
            String queue,
            String routingKey,
            String dlq,
            String dlqRoutingKey) {
    }

    public Route ruta(String nombre) {
        Route ruta = rutas == null ? null : rutas.get(nombre);
        if (ruta == null) {
            throw new IllegalStateException(
                    "La ruta '" + nombre + "' no está definida en pedidos360.rabbitmq.rutas");
        }
        return ruta;
    }
}
