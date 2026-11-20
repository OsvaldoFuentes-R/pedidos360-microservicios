package cl.duoc.pedidos360.bff.web;

import cl.duoc.pedidos360.bff.client.MicroserviciosClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Datos de apoyo para armar un pedido desde el frontend (productos y clientes disponibles). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final MicroserviciosClient microservicios;

    public CatalogoController(MicroserviciosClient microservicios) {
        this.microservicios = microservicios;
    }

    @GetMapping("/productos")
    public List<Map<String, Object>> productos() {
        return microservicios.listarProductos();
    }

    @GetMapping("/clientes")
    public List<Map<String, Object>> clientes() {
        return microservicios.listarClientes();
    }
}
