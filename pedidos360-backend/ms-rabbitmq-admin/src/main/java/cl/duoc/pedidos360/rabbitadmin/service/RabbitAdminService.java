package cl.duoc.pedidos360.rabbitadmin.service;

import cl.duoc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingInfo;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.ColaEstado;
import cl.duoc.pedidos360.rabbitadmin.dto.ColaInfo;
import cl.duoc.pedidos360.rabbitadmin.dto.ColaRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeInfo;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.TipoDestino;
import cl.duoc.pedidos360.rabbitadmin.error.ConflictoException;
import cl.duoc.pedidos360.rabbitadmin.error.OperacionNoPermitidaException;
import cl.duoc.pedidos360.rabbitadmin.error.RabbitNoDisponibleException;
import cl.duoc.pedidos360.rabbitadmin.error.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Crea y elimina colas, exchanges y bindings (por AMQP con AmqpAdmin) y los lista
 * (por la API HTTP de administración de RabbitMQ).
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RestClient management;
    private final RabbitAdminProperties props;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RestClient managementRestClient, RabbitAdminProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.management = managementRestClient;
        this.props = props;
    }

    // ------------------------------------------------------------------ colas

    public ColaEstado crearCola(ColaRequest datos) {
        ReglasRabbit.validarNombre(datos.nombre());
        ReglasRabbit.validarDeadLetter(datos.deadLetterExchange(), datos.deadLetterRoutingKey());

        Map<String, Object> argumentos = new HashMap<>();
        if (datos.deadLetterExchange() != null && !datos.deadLetterExchange().isBlank()) {
            argumentos.put("x-dead-letter-exchange", datos.deadLetterExchange());
            argumentos.put("x-dead-letter-routing-key", datos.deadLetterRoutingKey());
        }
        if (datos.ttlMs() != null) {
            argumentos.put("x-message-ttl", datos.ttlMs());
        }
        ejecutar("crear la cola " + datos.nombre(), () ->
                amqpAdmin.declareQueue(new Queue(datos.nombre(), datos.esDurable(), false, false, argumentos)));
        log.info("Cola creada: {} (argumentos: {})", datos.nombre(), argumentos);
        return estadoCola(datos.nombre());
    }

    public ColaEstado estadoCola(String nombre) {
        QueueInformation info = ejecutar("consultar la cola " + nombre, () -> amqpAdmin.getQueueInfo(nombre));
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new ColaEstado(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre, boolean soloSiVacia, boolean forzar) {
        if (!forzar && ReglasRabbit.colaProtegida(nombre, props.protegidos().prefijosColas())) {
            throw new OperacionNoPermitidaException("La cola '" + nombre
                    + "' es parte de la arquitectura: para eliminarla agrega ?forzar=true");
        }
        estadoCola(nombre); // 404 si no existe
        ejecutar("eliminar la cola " + nombre, () -> {
            amqpAdmin.deleteQueue(nombre, false, soloSiVacia);
            return null;
        });
        log.warn("Cola eliminada: {}", nombre);
    }

    public int vaciarCola(String nombre) {
        ColaEstado antes = estadoCola(nombre);
        ejecutar("vaciar la cola " + nombre, () -> {
            amqpAdmin.purgeQueue(nombre, false);
            return null;
        });
        log.warn("Cola vaciada: {} ({} mensajes)", nombre, antes.mensajes());
        return antes.mensajes();
    }

    public List<ColaInfo> listarColas() {
        return listarRemoto("/queues/" + vhost(), new ParameterizedTypeReference<List<ColaInfo>>() { });
    }

    // -------------------------------------------------------------- exchanges

    public void crearExchange(ExchangeRequest datos) {
        ReglasRabbit.validarNombre(datos.nombre());
        Map<String, Object> argumentos = new HashMap<>();
        if (datos.alternateExchange() != null && !datos.alternateExchange().isBlank()) {
            argumentos.put("alternate-exchange", datos.alternateExchange());
        }
        AbstractExchange exchange = switch (datos.tipo()) {
            case DIRECT -> new DirectExchange(datos.nombre(), datos.esDurable(), false, argumentos);
            case TOPIC -> new TopicExchange(datos.nombre(), datos.esDurable(), false, argumentos);
            case FANOUT -> new FanoutExchange(datos.nombre(), datos.esDurable(), false, argumentos);
            case HEADERS -> new HeadersExchange(datos.nombre(), datos.esDurable(), false, argumentos);
        };
        ejecutar("crear el exchange " + datos.nombre(), () -> {
            amqpAdmin.declareExchange(exchange);
            return null;
        });
        log.info("Exchange creado: {} ({})", datos.nombre(), datos.tipo());
    }

    public void eliminarExchange(String nombre, boolean forzar) {
        if (!forzar && ReglasRabbit.exchangeProtegido(nombre, props.protegidos().exchanges())) {
            throw new OperacionNoPermitidaException("El exchange '" + nombre
                    + "' es parte de la arquitectura: para eliminarlo agrega ?forzar=true");
        }
        boolean existe = listarExchanges().stream().anyMatch(e -> nombre.equals(e.name()));
        if (!existe) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        ejecutar("eliminar el exchange " + nombre, () -> amqpAdmin.deleteExchange(nombre));
        log.warn("Exchange eliminado: {}", nombre);
    }

    public List<ExchangeInfo> listarExchanges() {
        return listarRemoto("/exchanges/" + vhost(), new ParameterizedTypeReference<List<ExchangeInfo>>() { });
    }

    // --------------------------------------------------------------- bindings

    public void crearBinding(BindingRequest datos) {
        Binding binding = armar(datos);
        ejecutar("crear el binding", () -> {
            amqpAdmin.declareBinding(binding);
            return null;
        });
        log.info("Binding creado: {} -> {} [{}]", datos.exchange(), datos.destino(), datos.clave());
    }

    public void eliminarBinding(BindingRequest datos) {
        Binding binding = armar(datos);
        ejecutar("eliminar el binding", () -> {
            amqpAdmin.removeBinding(binding);
            return null;
        });
        log.warn("Binding eliminado: {} -> {} [{}]", datos.exchange(), datos.destino(), datos.clave());
    }

    public List<BindingInfo> listarBindings() {
        return listarRemoto("/bindings/" + vhost(), new ParameterizedTypeReference<List<BindingInfo>>() { });
    }

    private Binding armar(BindingRequest datos) {
        ReglasRabbit.validarNombre(datos.exchange());
        ReglasRabbit.validarNombre(datos.destino());
        Binding.DestinationType tipo = datos.tipoDestino() == TipoDestino.QUEUE
                ? Binding.DestinationType.QUEUE : Binding.DestinationType.EXCHANGE;
        return new Binding(datos.destino(), tipo, datos.exchange(), datos.clave(), null);
    }

    // ---------------------------------------------------------------- soporte

    private String vhost() {
        return URLEncoder.encode(props.vhost(), StandardCharsets.UTF_8); // "/" -> "%2F"
    }

    private <T> List<T> listarRemoto(String ruta, ParameterizedTypeReference<List<T>> tipo) {
        try {
            List<T> lista = management.get().uri(URI.create(props.managementUrl() + "/api" + ruta)).retrieve().body(tipo);
            return lista == null ? List.of() : lista;
        } catch (RestClientException e) {
            throw new RabbitNoDisponibleException("No se pudo consultar la API de administración de RabbitMQ", e);
        }
    }

    private <T> T ejecutar(String descripcion, Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (AmqpException e) {
            String causa = e.getMessage() == null ? "" : e.getMessage();
            if (causa.contains("PRECONDITION_FAILED") || (e.getCause() != null
                    && String.valueOf(e.getCause().getMessage()).contains("PRECONDITION_FAILED"))) {
                throw new ConflictoException("No se pudo " + descripcion
                        + ": ya existe con otros argumentos (PRECONDITION_FAILED)", e);
            }
            throw new RabbitNoDisponibleException("RabbitMQ no pudo " + descripcion + ": " + causa, e);
        }
    }
}
