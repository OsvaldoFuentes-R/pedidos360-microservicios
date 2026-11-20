package cl.duoc.pedidos360.kafkaadmin.web;

import cl.duoc.pedidos360.kafkaadmin.dto.ClusterInfo;
import cl.duoc.pedidos360.kafkaadmin.service.ClusterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka/cluster")
public class ClusterController {

    private final ClusterService servicio;

    public ClusterController(ClusterService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ClusterInfo describir() {
        return servicio.describir();
    }
}
