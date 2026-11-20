package cl.duoc.pedidos360.pedidos.messaging;

import cl.duoc.pedidos360.pedidos.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.pedidos.dto.PedidoEventoKafka;
import cl.duoc.pedidos360.pedidos.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Productor Kafka: publica en pedidos.eventos cada pedido registrado (lo consume ms-analitica). */
@Component
public class PedidoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final KafkaTopicsProperties topics;

    public PedidoKafkaPublisher(KafkaTemplate<String, Object> kafka, KafkaTopicsProperties topics) {
        this.kafka = kafka;
        this.topics = topics;
    }

    public void publicarPedidoRegistrado(Pedido pedido) {
        PedidoEventoKafka evento = new PedidoEventoKafka(
                UUID.randomUUID().toString(),
                PedidoEventoKafka.PEDIDO_REGISTRADO,
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getClienteNombre(),
                pedido.getTotal(),
                Instant.now().toString(),
                pedido.getItems().stream()
                        .map(i -> new PedidoEventoKafka.ItemEvento(i.getProductoId(), i.getNombreProducto(),
                                i.getCantidad(), i.getPrecioUnitario()))
                        .toList());

        kafka.send(topics.pedidosEventos(), String.valueOf(pedido.getId()), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("[KAFKA] No se pudo publicar el evento del pedido {}: {}",
                                pedido.getId(), error.getMessage());
                    } else {
                        log.info("[KAFKA] Evento {} publicado en {} (partición {}, offset {})",
                                evento.eventId(), topics.pedidosEventos(),
                                resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                    }
                });
    }
}
