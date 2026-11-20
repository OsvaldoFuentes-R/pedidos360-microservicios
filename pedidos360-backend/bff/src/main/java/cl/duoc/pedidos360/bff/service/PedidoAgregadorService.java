package cl.duoc.pedidos360.bff.service;

import cl.duoc.pedidos360.bff.client.MicroserviciosClient;
import cl.duoc.pedidos360.bff.dto.PedidoRemoto;
import cl.duoc.pedidos360.bff.dto.PedidoVista;
import cl.duoc.pedidos360.bff.dto.SolicitudPedido;
import cl.duoc.pedidos360.bff.dto.TicketRemoto;
import cl.duoc.pedidos360.bff.error.ServicioNoDisponibleException;
import cl.duoc.pedidos360.bff.messaging.RegistroPedidoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Patrón BFF: junta lo que el frontend necesita desde varios microservicios.
 * El estado que ve el usuario sale del ticket de despacho; si ms-despacho no responde,
 * el pedido se muestra igual con estado "Procesando" en vez de fallar toda la pantalla.
 */
@Service
public class PedidoAgregadorService {

    private static final Logger log = LoggerFactory.getLogger(PedidoAgregadorService.class);

    private final MicroserviciosClient microservicios;
    private final RegistroPedidoPublisher publisher;

    public PedidoAgregadorService(MicroserviciosClient microservicios, RegistroPedidoPublisher publisher) {
        this.microservicios = microservicios;
        this.publisher = publisher;
    }

    public void solicitar(SolicitudPedido solicitud) {
        publisher.publicar(solicitud);
    }

    public List<PedidoVista> listar() {
        List<PedidoRemoto> pedidos = microservicios.listarPedidos();
        Map<Long, TicketRemoto> tickets = ticketsPorPedido();
        return pedidos.stream()
                .map(p -> armar(p, tickets.get(p.id())))
                .toList();
    }

    public PedidoVista obtener(Long id) {
        PedidoRemoto pedido = microservicios.obtenerPedido(id);
        TicketRemoto ticket = null;
        try {
            ticket = microservicios.ticketDePedido(id);
        } catch (ServicioNoDisponibleException e) {
            log.warn("ms-despacho no respondió; el pedido {} se muestra sin ticket", id);
        }
        return armar(pedido, ticket);
    }

    private Map<Long, TicketRemoto> ticketsPorPedido() {
        try {
            return microservicios.listarTickets().stream()
                    .collect(Collectors.toMap(TicketRemoto::pedidoId, t -> t, (a, b) -> a, HashMap::new));
        } catch (ServicioNoDisponibleException e) {
            log.warn("ms-despacho no respondió; los pedidos se muestran como 'Procesando'");
            return Map.of();
        }
    }

    PedidoVista armar(PedidoRemoto pedido, TicketRemoto ticket) {
        List<PedidoVista.Linea> lineas = pedido.items().stream()
                .map(i -> new PedidoVista.Linea(i.nombreProducto(), i.cantidad(), i.precioUnitario()))
                .toList();
        String resumen = pedido.items().stream()
                .map(i -> i.cantidad() > 1 ? i.nombreProducto() + " x" + i.cantidad() : i.nombreProducto())
                .collect(Collectors.joining(", "));
        return new PedidoVista(pedido.id(), resumen, estadoVisible(pedido.estado(), ticket),
                pedido.clienteNombre(), pedido.total(), pedido.fecha(),
                ticket == null ? null : ticket.codigo(), lineas);
    }

    /** Textos que ya entiende el frontend ("Procesando", "En tránsito", "Entregado"). */
    String estadoVisible(String estadoPedido, TicketRemoto ticket) {
        if ("CANCELADO".equals(estadoPedido)) {
            return "Cancelado";
        }
        if (ticket == null || ticket.estado() == null) {
            return "Procesando";
        }
        return switch (ticket.estado()) {
            case "EN_RUTA" -> "En tránsito";
            case "ENTREGADO" -> "Entregado";
            default -> "Procesando";
        };
    }
}
