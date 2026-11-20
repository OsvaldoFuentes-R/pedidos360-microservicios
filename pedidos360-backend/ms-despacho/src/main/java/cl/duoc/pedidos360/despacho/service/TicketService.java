package cl.duoc.pedidos360.despacho.service;

import cl.duoc.pedidos360.despacho.dto.EventoTicket;
import cl.duoc.pedidos360.despacho.error.ConflictoException;
import cl.duoc.pedidos360.despacho.error.RecursoNoEncontradoException;
import cl.duoc.pedidos360.despacho.model.EstadoTicket;
import cl.duoc.pedidos360.despacho.messaging.TicketKafkaPublisher;
import cl.duoc.pedidos360.despacho.model.Ticket;
import cl.duoc.pedidos360.despacho.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository repositorio;
    private final TicketKafkaPublisher kafka;

    public TicketService(TicketRepository repositorio, TicketKafkaPublisher kafka) {
        this.repositorio = repositorio;
        this.kafka = kafka;
    }

    /**
     * Genera el ticket de un pedido. Un evento repetido no crea un segundo ticket, y uno
     * incompleto lanza IllegalArgumentException (el consumidor lo manda a la DLQ).
     *
     * @return el ticket creado, o null si el pedido ya tenía ticket
     */
    @Transactional
    public Ticket generar(EventoTicket evento) {
        if (evento == null || evento.pedidoId() == null) {
            throw new IllegalArgumentException("el evento no indica el pedidoId");
        }
        if (evento.direccionEntrega() == null || evento.direccionEntrega().isBlank()) {
            throw new IllegalArgumentException("el pedido " + evento.pedidoId() + " no tiene dirección de entrega");
        }
        if (evento.clienteNombre() == null || evento.clienteNombre().isBlank()) {
            throw new IllegalArgumentException("el pedido " + evento.pedidoId() + " no tiene nombre de cliente");
        }
        if (repositorio.existsByPedidoId(evento.pedidoId())) {
            log.info("El pedido {} ya tiene ticket; se ignora el evento repetido", evento.pedidoId());
            return null;
        }

        String codigo = String.format("TCK-%06d", evento.pedidoId());
        Ticket ticket = repositorio.save(new Ticket(evento.pedidoId(), codigo, evento.clienteNombre(),
                evento.direccionEntrega(), evento.totalItems()));
        log.info("[DESPACHO] Ticket {} generado para el pedido {} (entrega en: {})",
                ticket.getCodigo(), ticket.getPedidoId(), ticket.getDireccionEntrega());
        return ticket;
    }

    @Transactional(readOnly = true)
    public List<Ticket> listar() {
        return repositorio.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Ticket obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El ticket " + id + " no existe"));
    }

    @Transactional(readOnly = true)
    public Ticket obtenerPorPedido(Long pedidoId) {
        return repositorio.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido " + pedidoId + " no tiene ticket"));
    }

    /** Avanza el ticket un paso: GENERADO -> EN_RUTA -> ENTREGADO. Cualquier otro salto es un conflicto. */
    @Transactional
    public Ticket cambiarEstado(Long id, EstadoTicket nuevoEstado) {
        Ticket ticket = obtener(id);
        if (!ticket.getEstado().puedePasarA(nuevoEstado)) {
            throw new ConflictoException("No se puede pasar el ticket " + ticket.getCodigo()
                    + " de " + ticket.getEstado() + " a " + nuevoEstado);
        }
        EstadoTicket anterior = ticket.getEstado();
        ticket.cambiarEstado(nuevoEstado);
        Ticket guardado = repositorio.save(ticket);
        kafka.publicarCambioEstado(guardado, anterior);
        return guardado;
    }
}
