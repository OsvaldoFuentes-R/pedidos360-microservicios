package cl.duoc.pedidos360.rabbitadmin.web;

import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeInfo;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeRequest;
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

@RestController
@RequestMapping("/rabbit/exchanges")
public class ExchangeController {

    private final RabbitAdminService servicio;

    public ExchangeController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ExchangeInfo> listar() {
        return servicio.listarExchanges();
    }

    @PostMapping
    public ResponseEntity<Void> crear(@Valid @RequestBody ExchangeRequest datos) {
        servicio.crearExchange(datos);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre,
                                         @RequestParam(defaultValue = "false") boolean forzar) {
        servicio.eliminarExchange(nombre, forzar);
        return ResponseEntity.noContent().build();
    }
}
