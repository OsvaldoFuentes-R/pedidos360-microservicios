package cl.duoc.pedidos360.rabbitadmin.web;

import cl.duoc.pedidos360.rabbitadmin.dto.BindingInfo;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rabbit/bindings")
public class BindingController {

    private final RabbitAdminService servicio;

    public BindingController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<BindingInfo> listar() {
        return servicio.listarBindings();
    }

    @PostMapping
    public ResponseEntity<Void> crear(@Valid @RequestBody BindingRequest datos) {
        servicio.crearBinding(datos);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** DELETE con cuerpo: identifica el binding por exchange, destino, tipo y routing key. */
    @DeleteMapping
    public ResponseEntity<Void> eliminar(@Valid @RequestBody BindingRequest datos) {
        servicio.eliminarBinding(datos);
        return ResponseEntity.noContent().build();
    }
}
