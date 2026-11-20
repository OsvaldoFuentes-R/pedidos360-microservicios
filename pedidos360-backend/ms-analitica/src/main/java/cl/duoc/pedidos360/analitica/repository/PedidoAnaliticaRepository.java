package cl.duoc.pedidos360.analitica.repository;

import cl.duoc.pedidos360.analitica.model.PedidoAnalitica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface PedidoAnaliticaRepository extends JpaRepository<PedidoAnalitica, Long> {

    boolean existsByEventId(String eventId);

    @Query("select coalesce(sum(p.total), 0) from PedidoAnalitica p")
    BigDecimal sumaIngresos();

    @Query("select coalesce(sum(p.unidades), 0) from PedidoAnalitica p")
    long sumaUnidades();
}
