package cl.duoc.pedidos360.bff.web;

import cl.duoc.pedidos360.bff.dto.PedidoVista;
import cl.duoc.pedidos360.bff.dto.RespuestaRecepcion;
import cl.duoc.pedidos360.bff.dto.SolicitudPedido;
import cl.duoc.pedidos360.bff.service.PedidoAgregadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoAgregadorService servicio;

    public PedidoController(PedidoAgregadorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PedidoVista> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public PedidoVista obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /** El pedido no se crea aquí: se encola en RabbitMQ y ms-pedidos lo registra. Por eso responde 202. */
    @PostMapping
    public ResponseEntity<RespuestaRecepcion> crear(@Valid @RequestBody SolicitudPedido solicitud) {
        servicio.solicitar(solicitud);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RespuestaRecepcion("EN_COLA",
                "Pedido recibido; se está procesando. Consulta GET /api/pedidos para ver su estado."));
    }
}
