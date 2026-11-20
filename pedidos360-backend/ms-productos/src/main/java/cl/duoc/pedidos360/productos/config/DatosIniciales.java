package cl.duoc.pedidos360.productos.config;

import cl.duoc.pedidos360.productos.model.Producto;
import cl.duoc.pedidos360.productos.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Carga un catálogo de ejemplo al arrancar (la base H2 en memoria parte vacía). */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProductoRepository repositorio;

    public DatosIniciales(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }
        repositorio.saveAll(List.of(
                new Producto("Laptop HP 15", "Notebook 15 pulgadas, 16 GB RAM, SSD 512 GB",
                        new BigDecimal("549990.00"), 25, "Computación"),
                new Producto("Monitor LG 27", "Monitor 27 pulgadas Full HD IPS",
                        new BigDecimal("159990.00"), 40, "Computación"),
                new Producto("Teclado Mecánico", "Teclado mecánico retroiluminado",
                        new BigDecimal("39990.00"), 60, "Accesorios"),
                new Producto("Mouse Inalámbrico", "Mouse ergonómico 2.4 GHz",
                        new BigDecimal("14990.00"), 100, "Accesorios"),
                new Producto("Audífonos Bluetooth", "Audífonos con cancelación de ruido",
                        new BigDecimal("79990.00"), 30, "Audio"),
                new Producto("Webcam Full HD", "Cámara web 1080p con micrófono",
                        new BigDecimal("29990.00"), 5, "Accesorios")));
    }
}
