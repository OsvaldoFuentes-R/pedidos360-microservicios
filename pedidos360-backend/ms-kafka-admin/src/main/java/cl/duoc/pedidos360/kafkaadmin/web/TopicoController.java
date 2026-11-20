package cl.duoc.pedidos360.kafkaadmin.web;

import cl.duoc.pedidos360.kafkaadmin.dto.ConfiguracionRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.ParticionesRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicoInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicoRequest;
import cl.duoc.pedidos360.kafkaadmin.service.TopicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/kafka/topicos")
public class TopicoController {

    private final TopicoService servicio;

    public TopicoController(TopicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Set<String> listar() {
        return servicio.listar();
    }

    @GetMapping("/{nombre}")
    public TopicoInfo describir(@PathVariable String nombre) {
        return servicio.describir(nombre);
    }

    @PostMapping
    public ResponseEntity<TopicoInfo> crear(@Valid @RequestBody TopicoRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(datos));
    }

    @PutMapping("/{nombre}/configuracion")
    public TopicoInfo actualizarConfiguracion(@PathVariable String nombre,
                                              @Valid @RequestBody ConfiguracionRequest datos,
                                              @RequestParam(defaultValue = "false") boolean forzar) {
        return servicio.actualizarConfiguracion(nombre, datos.configuracion(), forzar);
    }

    @PutMapping("/{nombre}/particiones")
    public TopicoInfo aumentarParticiones(@PathVariable String nombre,
                                          @Valid @RequestBody ParticionesRequest datos,
                                          @RequestParam(defaultValue = "false") boolean forzar) {
        return servicio.aumentarParticiones(nombre, datos.particiones(), forzar);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre,
                                         @RequestParam(defaultValue = "false") boolean forzar) {
        servicio.eliminar(nombre, forzar);
        return ResponseEntity.noContent().build();
    }
}
