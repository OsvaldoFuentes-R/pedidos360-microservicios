package cl.duoc.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Último stock conocido de cada producto, según los movimientos de inventario. */
@Entity
@Table(name = "stock_producto")
public class StockProducto {

    @Id
    private Long productoId;

    @Column(nullable = false)
    private String nombre;

    private int stockActual;

    private LocalDateTime actualizadoEn;

    protected StockProducto() {
        // requerido por JPA
    }

    public StockProducto(Long productoId, String nombre) {
        this.productoId = productoId;
        this.nombre = nombre;
    }

    public void actualizar(String nombre, int stock) {
        this.nombre = nombre;
        this.stockActual = stock;
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getProductoId() { return productoId; }
    public String getNombre() { return nombre; }
    public int getStockActual() { return stockActual; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
