package cl.duoc.pedidos360.analitica.kafka;

import cl.duoc.pedidos360.analitica.service.AnaliticaInventarioService;
import cl.duoc.pedidos360.analitica.service.AuditoriaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consume inventario.movimientos: mantiene el stock actual por producto (grupo analitica-inventario) y audita. */
@Component
public class InventarioListener {

    private static final Logger log = LoggerFactory.getLogger(InventarioListener.class);

    private final EventoParser parser;
    private final AnaliticaInventarioService inventario;
    private final AuditoriaService auditoria;

    public InventarioListener(EventoParser parser, AnaliticaInventarioService inventario, AuditoriaService auditoria) {
        this.parser = parser;
        this.inventario = inventario;
        this.auditoria = auditoria;
    }

    @KafkaListener(id = "stock-inventario", topics = "${pedidos360.kafka.topics.inventario-movimientos}",
            groupId = "${pedidos360.kafka.grupos.inventario}")
    public void actualizarStock(ConsumerRecord<String, String> registro) {
        trazar("analitica-inventario", registro);
        inventario.procesar(parser.movimiento(registro.value()));
    }

    @KafkaListener(id = "auditoria-inventario", topics = "${pedidos360.kafka.topics.inventario-movimientos}",
            groupId = "${pedidos360.kafka.grupos.auditoria}")
    public void auditar(ConsumerRecord<String, String> registro) {
        trazar("analitica-auditoria", registro);
        auditoria.registrar(registro.topic(), registro.partition(), registro.offset(), registro.key(),
                parser.tipoDe(registro.value()), registro.value());
    }

    private void trazar(String grupo, ConsumerRecord<String, String> r) {
        log.info("[KAFKA] grupo={} topico={} particion={} offset={} clave={}", grupo, r.topic(), r.partition(),
                r.offset(), r.key());
    }
}
