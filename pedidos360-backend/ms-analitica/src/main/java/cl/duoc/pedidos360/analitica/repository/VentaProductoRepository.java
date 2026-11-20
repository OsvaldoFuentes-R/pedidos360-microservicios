package cl.duoc.pedidos360.analitica.repository;

import cl.duoc.pedidos360.analitica.model.VentaProducto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, Long> {

    @Query("select v from VentaProducto v order by v.unidades desc")
    List<VentaProducto> masVendidos(Pageable pagina);
}
