package cl.duoc.pedidos360.pedidos.web;

import cl.duoc.pedidos360.pedidos.model.Pedido;
import cl.duoc.pedidos360.pedidos.service.PedidoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de pedidos. Los pedidos NO se crean por REST: llegan por la cola pedidos.registro.
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Pedido> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Pedido obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<Pedido> listarPorCliente(@PathVariable Long clienteId) {
        return servicio.listarPorCliente(clienteId);
    }
}
