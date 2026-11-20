package cl.duoc.pedidos360.productos.web;

import cl.duoc.pedidos360.productos.model.Producto;
import cl.duoc.pedidos360.productos.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Producto> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody ProductoRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    /** Descuenta stock. 409 si no alcanza. Lo invoca ms-pedidos. */
    @PostMapping("/{id}/reservar")
    public Producto reservar(@PathVariable Long id, @Valid @RequestBody CantidadRequest datos) {
        return servicio.reservar(id, datos.cantidad());
    }

    /** Devuelve stock reservado. Lo invoca ms-pedidos como compensación. */
    @PostMapping("/{id}/liberar")
    public Producto liberar(@PathVariable Long id, @Valid @RequestBody CantidadRequest datos) {
        return servicio.liberar(id, datos.cantidad());
    }
}
