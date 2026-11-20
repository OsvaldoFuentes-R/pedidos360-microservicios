package cl.duoc.pedidos360.bff.messaging;

import cl.duoc.pedidos360.bff.config.RabbitMQProperties;
import cl.duoc.pedidos360.bff.dto.SolicitudPedido;
import cl.duoc.pedidos360.bff.error.ServicioNoDisponibleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica la solicitud de un pedido nuevo en la cola pedidos.registro, que consume ms-pedidos. */
@Component
public class RegistroPedidoPublisher {

    private static final Logger log = LoggerFactory.getLogger(RegistroPedidoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;
    private final ObjectMapper mapper;

    public RegistroPedidoPublisher(RabbitTemplate rabbitTemplate, RabbitMQProperties props, ObjectMapper mapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
        this.mapper = mapper;
    }

    public void publicar(SolicitudPedido solicitud) {
        RabbitMQProperties.Route ruta = props.ruta("registro");
        try {
            String json = mapper.writeValueAsString(solicitud);
            rabbitTemplate.convertAndSend(props.exchange(), ruta.routingKey(), json);
            log.info("[{}] Solicitud publicada (routing key '{}'): {}", ruta.queue(), ruta.routingKey(), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir la solicitud a JSON", e);
        } catch (AmqpException e) {
            throw new ServicioNoDisponibleException("RabbitMQ no está disponible; el pedido no fue enviado", e);
        }
    }
}
