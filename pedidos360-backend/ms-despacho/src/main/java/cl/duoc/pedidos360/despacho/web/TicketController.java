package cl.duoc.pedidos360.despacho.web;

import cl.duoc.pedidos360.despacho.model.Ticket;
import cl.duoc.pedidos360.despacho.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de tickets y avance de su estado. Los tickets se crean desde la cola pedidos.tickets. */
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService servicio;

    public TicketController(TicketService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Ticket> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Ticket obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @GetMapping("/pedido/{pedidoId}")
    public Ticket obtenerPorPedido(@PathVariable Long pedidoId) {
        return servicio.obtenerPorPedido(pedidoId);
    }

    @PutMapping("/{id}/estado")
    public Ticket cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest datos) {
        return servicio.cambiarEstado(id, datos.estado());
    }
}
