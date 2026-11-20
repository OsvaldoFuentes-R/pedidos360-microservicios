package cl.duoc.pedidos360.pedidos.messaging;

import cl.duoc.pedidos360.pedidos.config.RabbitMQProperties;
import cl.duoc.pedidos360.pedidos.dto.SolicitudPedido;
import cl.duoc.pedidos360.pedidos.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consume pedidos.registro: cada mensaje es una solicitud de pedido en JSON.
 * Si es válida se registra (ACK); si no (JSON roto, cliente o producto inexistente,
 * sin stock) se rechaza (NACK) y pasa a pedidos.registro.dlq.
 */
@Component
public class RegistroPedidoConsumer extends ConsumidorConAck {

    private final PedidoService pedidoService;
    private final ObjectMapper mapper;

    public RegistroPedidoConsumer(RabbitMQProperties props, PedidoService pedidoService, ObjectMapper mapper) {
        super(props.ruta("registro").queue());
        this.pedidoService = pedidoService;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "${pedidos360.rabbitmq.rutas.registro.queue}", ackMode = "MANUAL")
    public void recibir(String mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesar(mensaje, channel, deliveryTag, this::registrar);
    }

    private void registrar(String mensaje) throws Exception {
        SolicitudPedido solicitud = mapper.readValue(mensaje, SolicitudPedido.class);
        pedidoService.registrar(solicitud);
    }
}
