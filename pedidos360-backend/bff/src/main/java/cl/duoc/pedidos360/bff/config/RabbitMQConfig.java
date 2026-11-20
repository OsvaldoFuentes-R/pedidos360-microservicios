package cl.duoc.pedidos360.bff.config;

import cl.duoc.pedidos360.bff.config.RabbitMQProperties.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Topología de RabbitMQ de Pedidos360 (exchanges, colas, DLQ y bindings).
 *
 * <p>Todos los servicios declaran el mismo exchange principal, el DLX y el exchange alternativo
 * con argumentos idénticos (si difirieran, RabbitMQ respondería PRECONDITION_FAILED), y cada uno
 * agrega solo las colas listadas en "pedidos360.rabbitmq.declarar".</p>
 *
 * <pre>
 *  pedidos360.exchange (direct) --pedido.registro-----&gt; pedidos.registro
 *                               --pedido.notificacion-&gt; pedidos.notificaciones
 *                               --pedido.ticket-------&gt; pedidos.tickets
 *  (sin binding) --&gt; pedidos360.unroutable (fanout) --&gt; pedidos.noruteados
 *  (rechazado)   --&gt; pedidos360.dlx (topic) --&gt; pedidos.*.dlq
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    private static final String ARG_ALTERNATE_EXCHANGE = "alternate-exchange";
    private static final String ARG_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String ARG_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    @Bean
    public Declarables topologiaPedidos360(RabbitMQProperties props) {
        List<Declarable> declarables = new ArrayList<>();

        // Exchange principal: los mensajes sin binding se envían al exchange alternativo.
        DirectExchange principal = new DirectExchange(props.exchange(), true, false,
                Map.<String, Object>of(ARG_ALTERNATE_EXCHANGE, props.unroutableExchange()));
        // Dead Letter Exchange: recibe los mensajes rechazados (NACK) por los consumidores.
        TopicExchange dlx = new TopicExchange(props.dlx(), true, false);
        // Exchange alternativo y su cola: nada se pierde en silencio.
        FanoutExchange alterno = new FanoutExchange(props.unroutableExchange(), true, false);
        Queue sinRuta = QueueBuilder.durable(props.unroutableQueue()).build();

        declarables.add(principal);
        declarables.add(dlx);
        declarables.add(alterno);
        declarables.add(sinRuta);
        declarables.add(BindingBuilder.bind(sinRuta).to(alterno));

        for (String nombre : props.declarar()) {
            Route ruta = props.ruta(nombre);

            Queue cola = QueueBuilder.durable(ruta.queue())
                    .withArgument(ARG_DEAD_LETTER_EXCHANGE, props.dlx())
                    .withArgument(ARG_DEAD_LETTER_ROUTING_KEY, ruta.dlqRoutingKey())
                    .build();
            Queue dlq = QueueBuilder.durable(ruta.dlq()).build();
            Binding bindingCola = BindingBuilder.bind(cola).to(principal).with(ruta.routingKey());
            Binding bindingDlq = BindingBuilder.bind(dlq).to(dlx).with(ruta.dlqRoutingKey());

            declarables.add(cola);
            declarables.add(dlq);
            declarables.add(bindingCola);
            declarables.add(bindingDlq);
        }

        log.info("Topología RabbitMQ lista para declarar: exchange '{}', rutas {}", props.exchange(), props.declarar());
        return new Declarables(declarables);
    }
}
