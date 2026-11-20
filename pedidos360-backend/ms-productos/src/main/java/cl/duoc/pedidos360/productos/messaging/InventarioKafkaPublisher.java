package cl.duoc.pedidos360.productos.messaging;

import cl.duoc.pedidos360.productos.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.productos.dto.MovimientoInventarioKafka;
import cl.duoc.pedidos360.productos.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka: publica cada movimiento de stock. Nunca lanza excepciones hacia el negocio:
 * si Kafka no responde, solo se registra el error (el stock ya se descontó correctamente).
 */
@Component
public class InventarioKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventarioKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final KafkaTopicsProperties topics;

    public InventarioKafkaPublisher(KafkaTemplate<String, Object> kafka, KafkaTopicsProperties topics) {
        this.kafka = kafka;
        this.topics = topics;
    }

    public void publicar(String tipo, Producto producto, int cantidad) {
        try {
            MovimientoInventarioKafka evento = new MovimientoInventarioKafka(UUID.randomUUID().toString(), tipo,
                    producto.getId(), producto.getNombre(), cantidad, producto.getStock(), Instant.now().toString());
            kafka.send(topics.inventarioMovimientos(), String.valueOf(producto.getId()), evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("[KAFKA] No se pudo publicar el movimiento {} del producto {}: {}",
                                    tipo, producto.getId(), error.getMessage());
                        } else {
                            log.info("[KAFKA] Movimiento {} del producto {} publicado (partición {}, offset {})",
                                    tipo, producto.getId(), resultado.getRecordMetadata().partition(),
                                    resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException e) {
            log.error("[KAFKA] Falló el envío del movimiento {} del producto {}: {}", tipo, producto.getId(), e.getMessage());
        }
    }
}
