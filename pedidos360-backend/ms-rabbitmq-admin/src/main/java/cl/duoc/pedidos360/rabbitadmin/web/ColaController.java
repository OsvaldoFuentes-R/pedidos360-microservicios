package cl.duoc.pedidos360.rabbitadmin.web;

import cl.duoc.pedidos360.rabbitadmin.dto.ColaEstado;
import cl.duoc.pedidos360.rabbitadmin.dto.ColaInfo;
import cl.duoc.pedidos360.rabbitadmin.dto.ColaRequest;
import cl.duoc.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rabbit/colas")
public class ColaController {

    private final RabbitAdminService servicio;

    public ColaController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ColaInfo> listar() {
        return servicio.listarColas();
    }

    @GetMapping("/{nombre}")
    public ColaEstado estado(@PathVariable String nombre) {
        return servicio.estadoCola(nombre);
    }

    @PostMapping
    public ResponseEntity<ColaEstado> crear(@Valid @RequestBody ColaRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearCola(datos));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre,
                                         @RequestParam(defaultValue = "false") boolean soloSiVacia,
                                         @RequestParam(defaultValue = "false") boolean forzar) {
        servicio.eliminarCola(nombre, soloSiVacia, forzar);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{nombre}/mensajes")
    public Map<String, Object> vaciar(@PathVariable String nombre) {
        return Map.of("cola", nombre, "mensajesEliminados", servicio.vaciarCola(nombre));
    }
}
