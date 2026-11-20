package cl.duoc.pedidos360.productos.repository;

import cl.duoc.pedidos360.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Descuenta stock de forma atómica: la condición "stock >= cantidad" va dentro del
     * UPDATE, así dos pedidos simultáneos no pueden dejar el stock en negativo.
     *
     * @return filas afectadas (0 si el producto no existe o no alcanza el stock)
     */
    @Modifying(clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Devuelve stock (compensación cuando un pedido no pudo completarse). */
    @Modifying(clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock + :cantidad where p.id = :id")
    int reponerStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
