package cl.duoc.pedidos360.kafkaadmin.web;

import cl.duoc.pedidos360.kafkaadmin.dto.GrupoInfo;
import cl.duoc.pedidos360.kafkaadmin.service.GrupoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/kafka/grupos")
public class GrupoController {

    private final GrupoService servicio;

    public GrupoController(GrupoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<String> listar() {
        return servicio.listar();
    }

    @GetMapping("/{grupo}")
    public GrupoInfo describir(@PathVariable String grupo) {
        return servicio.describir(grupo);
    }

    @DeleteMapping("/{grupo}")
    public ResponseEntity<Void> eliminar(@PathVariable String grupo) {
        servicio.eliminar(grupo);
        return ResponseEntity.noContent().build();
    }
}
