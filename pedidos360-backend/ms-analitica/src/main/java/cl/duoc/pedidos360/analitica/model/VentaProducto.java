package cl.duoc.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Unidades vendidas acumuladas por producto. */
@Entity
@Table(name = "ventas_producto")
public class VentaProducto {

    @Id
    private Long productoId;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private long unidades;

    protected VentaProducto() {
        // requerido por JPA
    }

    public VentaProducto(Long productoId, String nombre) {
        this.productoId = productoId;
        this.nombre = nombre;
    }

    public void sumar(int cantidad) {
        this.unidades += cantidad;
    }

    public Long getProductoId() { return productoId; }
    public String getNombre() { return nombre; }
    public long getUnidades() { return unidades; }
}
