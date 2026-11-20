package cl.duoc.pedidos360.despacho.messaging;

import cl.duoc.pedidos360.despacho.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.despacho.dto.TicketEventoKafka;
import cl.duoc.pedidos360.despacho.model.EstadoTicket;
import cl.duoc.pedidos360.despacho.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka: publica cada cambio de estado de un ticket. Nunca lanza excepciones hacia el negocio:
 * si Kafka no responde, solo se registra el error (el cambio de estado ya quedó guardado).
 */
@Component
public class TicketKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final KafkaTopicsProperties topics;

    public TicketKafkaPublisher(KafkaTemplate<String, Object> kafka, KafkaTopicsProperties topics) {
        this.kafka = kafka;
        this.topics = topics;
    }

    public void publicarCambioEstado(Ticket ticket, EstadoTicket estadoAnterior) {
        try {
            TicketEventoKafka evento = new TicketEventoKafka(UUID.randomUUID().toString(),
                    TicketEventoKafka.TICKET_ESTADO_CAMBIADO, ticket.getPedidoId(), ticket.getCodigo(),
                    estadoAnterior.name(), ticket.getEstado().name(), Instant.now().toString());
            kafka.send(topics.pedidosEventos(), String.valueOf(ticket.getPedidoId()), evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("[KAFKA] No se pudo publicar el cambio del ticket {}: {}",
                                    ticket.getCodigo(), error.getMessage());
                        } else {
                            log.info("[KAFKA] Ticket {} {} -> {} publicado en {} (partición {}, offset {})",
                                    ticket.getCodigo(), estadoAnterior, ticket.getEstado(), topics.pedidosEventos(),
                                    resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException e) {
            log.error("[KAFKA] No se pudo enviar el cambio del ticket {}: {}", ticket.getCodigo(), e.getMessage());
        }
    }
}
