package cl.duoc.pedidos360.notificaciones.web;

import cl.duoc.pedidos360.notificaciones.model.Notificacion;
import cl.duoc.pedidos360.notificaciones.service.NotificacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de notificaciones. Se crean solo a partir de la cola pedidos.notificaciones. */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService servicio;

    public NotificacionController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Notificacion> listar() {
        return servicio.listar();
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<Notificacion> listarPorPedido(@PathVariable Long pedidoId) {
        return servicio.listarPorPedido(pedidoId);
    }
}
