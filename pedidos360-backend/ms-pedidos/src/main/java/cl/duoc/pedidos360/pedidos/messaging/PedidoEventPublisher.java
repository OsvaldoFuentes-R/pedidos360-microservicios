package cl.duoc.pedidos360.pedidos.messaging;

import cl.duoc.pedidos360.pedidos.config.RabbitMQProperties;
import cl.duoc.pedidos360.pedidos.dto.EventoNotificacion;
import cl.duoc.pedidos360.pedidos.dto.EventoTicket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica en RabbitMQ los eventos que genera un pedido registrado. */
@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;
    private final ObjectMapper mapper;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, RabbitMQProperties props, ObjectMapper mapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
        this.mapper = mapper;
    }

    public void publicarNotificacion(EventoNotificacion evento) {
        enviar(props.ruta("notificaciones"), evento);
    }

    public void publicarTicket(EventoTicket evento) {
        enviar(props.ruta("tickets"), evento);
    }

    private void enviar(RabbitMQProperties.Route ruta, Object evento) {
        String json;
        try {
            json = mapper.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir el evento a JSON", e);
        }
        rabbitTemplate.convertAndSend(props.exchange(), ruta.routingKey(), json);
        log.info("[{}] Evento publicado (routing key '{}'): {}", ruta.queue(), ruta.routingKey(), json);
    }
}
