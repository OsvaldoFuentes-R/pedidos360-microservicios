package cl.duoc.pedidos360.despacho.messaging;

import cl.duoc.pedidos360.despacho.config.RabbitMQProperties;
import cl.duoc.pedidos360.despacho.dto.EventoTicket;
import cl.duoc.pedidos360.despacho.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consume pedidos.tickets. Un evento válido genera el ticket de despacho (ACK);
 * uno roto o incompleto se rechaza (NACK) y pasa a pedidos.tickets.dlq.
 */
@Component
public class TicketConsumer extends ConsumidorConAck {

    private final TicketService servicio;
    private final ObjectMapper mapper;

    public TicketConsumer(RabbitMQProperties props, TicketService servicio, ObjectMapper mapper) {
        super(props.ruta("tickets").queue());
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "${pedidos360.rabbitmq.rutas.tickets.queue}", ackMode = "MANUAL")
    public void recibir(String mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesar(mensaje, channel, deliveryTag, this::generar);
    }

    private void generar(String mensaje) throws Exception {
        EventoTicket evento = mapper.readValue(mensaje, EventoTicket.class);
        servicio.generar(evento);
    }
}
