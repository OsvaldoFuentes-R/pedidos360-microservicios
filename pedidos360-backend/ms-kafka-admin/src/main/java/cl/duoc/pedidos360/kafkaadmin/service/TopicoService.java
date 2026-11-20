package cl.duoc.pedidos360.kafkaadmin.service;

import cl.duoc.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicoInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicoRequest;
import cl.duoc.pedidos360.kafkaadmin.error.OperacionNoPermitidaException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Crear, listar, describir, modificar y eliminar tópicos. */
@Service
public class TopicoService {

    private static final Logger log = LoggerFactory.getLogger(TopicoService.class);

    private final AdminClient admin;
    private final KafkaAdminOperaciones kafka;
    private final KafkaAdminProperties props;

    public TopicoService(AdminClient admin, KafkaAdminOperaciones kafka, KafkaAdminProperties props) {
        this.admin = admin;
        this.kafka = kafka;
        this.props = props;
    }

    public Set<String> listar() {
        return new TreeSet<>(kafka.esperar(admin.listTopics().names(), "listar tópicos"));
    }

    public TopicoInfo describir(String nombre) {
        TopicDescription descripcion = kafka.esperar(
                admin.describeTopics(List.of(nombre)).topicNameValues().get(nombre), "tópico " + nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = kafka.esperar(admin.describeConfigs(List.of(recurso)).values().get(recurso),
                "configuración del tópico " + nombre);

        Map<String, String> configuracion = new TreeMap<>();
        for (ConfigEntry entrada : config.entries()) {
            // Solo se muestran las que alguien cambió a propósito (no las que son valor por defecto)
            if (entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                configuracion.put(entrada.name(), entrada.value());
            }
        }

        List<TopicoInfo.ParticionInfo> detalle = new ArrayList<>();
        for (TopicPartitionInfo p : descripcion.partitions()) {
            detalle.add(new TopicoInfo.ParticionInfo(p.partition(),
                    p.leader() == null ? -1 : p.leader().id(), ids(p.replicas()), ids(p.isr())));
        }
        int replicas = detalle.isEmpty() ? 0 : detalle.get(0).replicas().size();
        return new TopicoInfo(nombre, detalle.size(), replicas, descripcion.isInternal(), detalle, configuracion);
    }

    public TopicoInfo crear(TopicoRequest datos) {
        ReglasTopico.validarNombre(datos.nombre());
        ReglasTopico.validarConfiguracion(datos.configuracion());
        ReglasTopico.validarReplicas(datos.replicas(), cantidadBrokers());

        NewTopic nuevo = new NewTopic(datos.nombre(), datos.particiones(), datos.replicas());
        if (datos.configuracion() != null && !datos.configuracion().isEmpty()) {
            nuevo.configs(datos.configuracion());
        }
        kafka.esperar(admin.createTopics(List.of(nuevo)).all(), "crear el tópico " + datos.nombre());
        log.info("Tópico creado: {} ({} particiones, {} réplicas)", datos.nombre(), datos.particiones(), datos.replicas());
        return describir(datos.nombre());
    }

    /** Crea el tópico solo si no existe (se usa al iniciar el servicio). */
    public void crearSiNoExiste(KafkaAdminProperties.TopicoInicial def) {
        if (listar().contains(def.nombre())) {
            log.info("El tópico {} ya existe", def.nombre());
            return;
        }
        crear(new TopicoRequest(def.nombre(), def.particiones(), def.replicas(), def.configuracion()));
    }

    public TopicoInfo actualizarConfiguracion(String nombre, Map<String, String> cambios, boolean forzar) {
        exigirNoProtegido(nombre, forzar, "modificar");
        ReglasTopico.validarConfiguracion(cambios);
        describir(nombre); // si no existe, responde 404

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        List<AlterConfigOp> operaciones = new ArrayList<>();
        cambios.forEach((clave, valor) ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
        kafka.esperar(admin.incrementalAlterConfigs(Map.of(recurso, (Collection<AlterConfigOp>) operaciones)).all(),
                "modificar la configuración de " + nombre);
        log.info("Configuración de {} actualizada: {}", nombre, cambios);
        return describir(nombre);
    }

    public TopicoInfo aumentarParticiones(String nombre, int nuevoTotal, boolean forzar) {
        exigirNoProtegido(nombre, forzar, "cambiar las particiones de");
        int actual = describir(nombre).particiones();
        if (nuevoTotal <= actual) {
            throw new IllegalArgumentException("El tópico ya tiene " + actual
                    + " particiones; Kafka solo permite aumentarlas (pediste " + nuevoTotal + ")");
        }
        kafka.esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(nuevoTotal))).all(),
                "aumentar las particiones de " + nombre);
        log.info("Tópico {}: particiones {} -> {}", nombre, actual, nuevoTotal);
        return describir(nombre);
    }

    public void eliminar(String nombre, boolean forzar) {
        exigirNoProtegido(nombre, forzar, "eliminar");
        describir(nombre);
        kafka.esperar(admin.deleteTopics(List.of(nombre)).all(), "eliminar el tópico " + nombre);
        log.warn("Tópico eliminado: {}", nombre);
    }

    private void exigirNoProtegido(String nombre, boolean forzar, String accion) {
        if (!forzar && ReglasTopico.esProtegido(nombre, props.admin().protegidos())) {
            throw new OperacionNoPermitidaException("Para " + accion + " el tópico '" + nombre
                    + "' (parte de la arquitectura) agrega ?forzar=true");
        }
    }

    private int cantidadBrokers() {
        return kafka.esperar(admin.describeCluster().nodes(), "consultar los brokers").size();
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }
}
