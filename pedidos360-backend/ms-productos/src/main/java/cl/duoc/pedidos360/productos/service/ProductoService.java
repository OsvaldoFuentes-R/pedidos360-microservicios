package cl.duoc.pedidos360.productos.service;

import cl.duoc.pedidos360.productos.dto.MovimientoInventarioKafka;
import cl.duoc.pedidos360.productos.error.RecursoNoEncontradoException;
import cl.duoc.pedidos360.productos.error.StockInsuficienteException;
import cl.duoc.pedidos360.productos.messaging.InventarioKafkaPublisher;
import cl.duoc.pedidos360.productos.model.Producto;
import cl.duoc.pedidos360.productos.repository.ProductoRepository;
import cl.duoc.pedidos360.productos.web.ProductoRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repositorio;
    private final InventarioKafkaPublisher kafka;

    public ProductoService(ProductoRepository repositorio, InventarioKafkaPublisher kafka) {
        this.repositorio = repositorio;
        this.kafka = kafka;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto " + id + " no existe"));
    }

    @Transactional
    public Producto crear(ProductoRequest datos) {
        Producto producto = new Producto(datos.nombre(), datos.descripcion(), datos.precio(),
                datos.stock(), datos.categoria());
        return repositorio.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest datos) {
        Producto producto = obtener(id);
        producto.setNombre(datos.nombre());
        producto.setDescripcion(datos.descripcion());
        producto.setPrecio(datos.precio());
        producto.setStock(datos.stock());
        producto.setCategoria(datos.categoria());
        return repositorio.save(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException("El producto " + id + " no existe");
        }
        repositorio.deleteById(id);
    }

    /** Descuenta stock (lo usa ms-pedidos al registrar un pedido). */
    @Transactional
    public Producto reservar(Long id, int cantidad) {
        if (repositorio.descontarStock(id, cantidad) == 0) {
            // 0 filas: o el producto no existe, o no alcanza el stock
            Producto producto = obtener(id);
            throw new StockInsuficienteException("Stock insuficiente de '" + producto.getNombre()
                    + "': disponible " + producto.getStock() + ", solicitado " + cantidad);
        }
        Producto actualizado = obtener(id);
        kafka.publicar(MovimientoInventarioKafka.RESERVA, actualizado, cantidad);
        return actualizado;
    }

    /** Devuelve stock reservado (compensación si el pedido falla a medio camino). */
    @Transactional
    public Producto liberar(Long id, int cantidad) {
        if (repositorio.reponerStock(id, cantidad) == 0) {
            throw new RecursoNoEncontradoException("El producto " + id + " no existe");
        }
        Producto actualizado = obtener(id);
        kafka.publicar(MovimientoInventarioKafka.LIBERACION, actualizado, cantidad);
        return actualizado;
    }
}
