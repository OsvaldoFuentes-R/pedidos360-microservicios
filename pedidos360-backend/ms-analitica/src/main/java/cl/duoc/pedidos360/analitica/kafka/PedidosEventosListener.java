package cl.duoc.pedidos360.analitica.kafka;

import cl.duoc.pedidos360.analitica.service.AnaliticaPedidosService;
import cl.duoc.pedidos360.analitica.service.AuditoriaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume pedidos.eventos con DOS consumer groups. Cada grupo recibe todos los mensajes del tópico:
 *  - analitica-kpis: calcula ingresos, ticket promedio y productos más vendidos;
 *  - analitica-auditoria: guarda la línea de tiempo de eventos.
 * Dentro de cada grupo, si se levantan varias instancias del servicio, las 3 particiones se reparten entre ellas.
 * La lógica de negocio vive en los servicios; esta clase solo recibe, valida y delega.
 */
@Component
public class PedidosEventosListener {

    private static final String PEDIDO_REGISTRADO = "PEDIDO_REGISTRADO";

    private static final Logger log = LoggerFactory.getLogger(PedidosEventosListener.class);

    private final EventoParser parser;
    private final AnaliticaPedidosService analitica;
    private final AuditoriaService auditoria;

    public PedidosEventosListener(EventoParser parser, AnaliticaPedidosService analitica, AuditoriaService auditoria) {
        this.parser = parser;
        this.analitica = analitica;
        this.auditoria = auditoria;
    }

    @KafkaListener(id = "kpis-pedidos", topics = "${pedidos360.kafka.topics.pedidos-eventos}",
            groupId = "${pedidos360.kafka.grupos.kpis}")
    public void calcularKpis(ConsumerRecord<String, String> registro) {
        trazar("analitica-kpis", registro);
        // pedidos.eventos también trae TICKET_ESTADO_CAMBIADO (ms-despacho): no afecta los KPIs de ventas.
        String tipo = parser.tipoDe(registro.value());
        if (!"DESCONOCIDO".equals(tipo) && !PEDIDO_REGISTRADO.equals(tipo)) {
            log.info("[KPI] Evento {} no aplica a los KPIs; se omite", tipo);
            return;
        }
        analitica.procesar(parser.pedido(registro.value()));
    }

    @KafkaListener(id = "auditoria-pedidos", topics = "${pedidos360.kafka.topics.pedidos-eventos}",
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
