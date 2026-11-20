package cl.duoc.pedidos360.analitica.web;

import cl.duoc.pedidos360.analitica.dto.KpisResumen;
import cl.duoc.pedidos360.analitica.dto.ProductoTop;
import cl.duoc.pedidos360.analitica.dto.StockCritico;
import cl.duoc.pedidos360.analitica.model.EventoAuditoria;
import cl.duoc.pedidos360.analitica.service.AnaliticaInventarioService;
import cl.duoc.pedidos360.analitica.service.AnaliticaPedidosService;
import cl.duoc.pedidos360.analitica.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultas sobre lo calculado a partir de los eventos Kafka. */
@RestController
@RequestMapping("/analitica")
public class AnaliticaController {

    private final AnaliticaPedidosService pedidos;
    private final AnaliticaInventarioService inventario;
    private final AuditoriaService auditoria;

    public AnaliticaController(AnaliticaPedidosService pedidos, AnaliticaInventarioService inventario,
                               AuditoriaService auditoria) {
        this.pedidos = pedidos;
        this.inventario = inventario;
        this.auditoria = auditoria;
    }

    @GetMapping("/kpis")
    public KpisResumen kpis() {
        return pedidos.kpis();
    }

    @GetMapping("/top-productos")
    public List<ProductoTop> topProductos(@RequestParam(defaultValue = "5") int limite) {
        return pedidos.masVendidos(limite);
    }

    @GetMapping("/stock-critico")
    public List<StockCritico> stockCritico(@RequestParam(defaultValue = "10") int umbral) {
        return inventario.stockCritico(umbral);
    }

    @GetMapping("/auditoria")
    public List<EventoAuditoria> auditoria(@RequestParam(required = false) String topico,
                                           @RequestParam(defaultValue = "50") int limite) {
        return auditoria.ultimos(topico, limite);
    }
}
