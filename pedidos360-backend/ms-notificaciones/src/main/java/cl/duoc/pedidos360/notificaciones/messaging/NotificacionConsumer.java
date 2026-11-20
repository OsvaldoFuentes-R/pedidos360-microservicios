package cl.duoc.pedidos360.notificaciones.messaging;

import cl.duoc.pedidos360.notificaciones.config.RabbitMQProperties;
import cl.duoc.pedidos360.notificaciones.dto.EventoNotificacion;
import cl.duoc.pedidos360.notificaciones.service.NotificacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consume pedidos.notificaciones. Un evento válido se guarda como notificación (ACK);
 * uno roto o incompleto se rechaza (NACK) y pasa a pedidos.notificaciones.dlq.
 */
@Component
public class NotificacionConsumer extends ConsumidorConAck {

    private final NotificacionService servicio;
    private final ObjectMapper mapper;

    public NotificacionConsumer(RabbitMQProperties props, NotificacionService servicio, ObjectMapper mapper) {
        super(props.ruta("notificaciones").queue());
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "${pedidos360.rabbitmq.rutas.notificaciones.queue}", ackMode = "MANUAL")
    public void recibir(String mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesar(mensaje, channel, deliveryTag, this::registrar);
    }

    private void registrar(String mensaje) throws Exception {
        EventoNotificacion evento = mapper.readValue(mensaje, EventoNotificacion.class);
        servicio.registrar(evento);
    }
}
