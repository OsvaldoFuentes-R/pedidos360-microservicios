package cl.duoc.pedidos360.notificaciones.service;

import cl.duoc.pedidos360.notificaciones.dto.EventoNotificacion;
import cl.duoc.pedidos360.notificaciones.model.Notificacion;
import cl.duoc.pedidos360.notificaciones.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository repositorio;

    public NotificacionService(NotificacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra la notificación de un pedido. Si el evento llega repetido (RabbitMQ garantiza
     * "al menos una vez") no se duplica. Un evento incompleto lanza IllegalArgumentException,
     * con lo que el consumidor lo rechaza y termina en la DLQ.
     *
     * @return la notificación creada, o null si el pedido ya estaba notificado
     */
    @Transactional
    public Notificacion registrar(EventoNotificacion evento) {
        if (evento == null || evento.pedidoId() == null) {
            throw new IllegalArgumentException("el evento no indica el pedidoId");
        }
        if (evento.clienteEmail() == null || evento.clienteEmail().isBlank()) {
            throw new IllegalArgumentException("el evento del pedido " + evento.pedidoId()
                    + " no tiene email de destino");
        }
        if (evento.mensaje() == null || evento.mensaje().isBlank()) {
            throw new IllegalArgumentException("el evento del pedido " + evento.pedidoId()
                    + " no tiene mensaje");
        }
        if (repositorio.existsByPedidoId(evento.pedidoId())) {
            log.info("El pedido {} ya estaba notificado; se ignora el evento repetido", evento.pedidoId());
            return null;
        }

        Notificacion notificacion = repositorio.save(new Notificacion(
                evento.pedidoId(), evento.clienteId(), evento.clienteEmail(),
                "Pedido #" + evento.pedidoId() + " registrado", evento.mensaje(), "EMAIL"));
        log.info("[NOTIFICACION] Correo (simulado) a {}: {}", notificacion.getDestinatario(), notificacion.getMensaje());
        return notificacion;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listar() {
        return repositorio.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarPorPedido(Long pedidoId) {
        return repositorio.findByPedidoIdOrderByIdDesc(pedidoId);
    }
}
