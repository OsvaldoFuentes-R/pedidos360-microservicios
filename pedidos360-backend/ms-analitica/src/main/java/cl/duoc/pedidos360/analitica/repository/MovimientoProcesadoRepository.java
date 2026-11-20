package cl.duoc.pedidos360.analitica.repository;

import cl.duoc.pedidos360.analitica.model.MovimientoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoProcesadoRepository extends JpaRepository<MovimientoProcesado, Long> {

    boolean existsByEventId(String eventId);
}
