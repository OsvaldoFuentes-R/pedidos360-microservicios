package cl.duoc.pedidos360.analitica.repository;

import cl.duoc.pedidos360.analitica.model.StockProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockProductoRepository extends JpaRepository<StockProducto, Long> {

    List<StockProducto> findByStockActualLessThanEqualOrderByStockActualAsc(int umbral);
}
