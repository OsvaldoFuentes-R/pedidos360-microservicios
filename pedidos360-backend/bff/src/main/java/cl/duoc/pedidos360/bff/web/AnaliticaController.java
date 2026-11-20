package cl.duoc.pedidos360.bff.web;

import cl.duoc.pedidos360.bff.client.MicroserviciosClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Reportería y auditoría para el frontend: datos que ms-analitica calcula a partir de los eventos Kafka. */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final MicroserviciosClient microservicios;

    public AnaliticaController(MicroserviciosClient microservicios) {
        this.microservicios = microservicios;
    }

    @GetMapping("/kpis")
    public Object kpis() {
        return microservicios.analitica("/analitica/kpis", Map.of());
    }

    @GetMapping("/top-productos")
    public Object topProductos(@RequestParam(defaultValue = "5") int limite) {
        return microservicios.analitica("/analitica/top-productos", Map.of("limite", limite));
    }

    @GetMapping("/stock-critico")
    public Object stockCritico(@RequestParam(defaultValue = "10") int umbral) {
        return microservicios.analitica("/analitica/stock-critico", Map.of("umbral", umbral));
    }

    @GetMapping("/auditoria")
    public Object auditoria(@RequestParam(required = false) String topico,
                            @RequestParam(defaultValue = "50") int limite) {
        Map<String, Object> parametros = new java.util.HashMap<>();
        parametros.put("topico", topico);
        parametros.put("limite", limite);
        return microservicios.analitica("/analitica/auditoria", parametros);
    }
}
