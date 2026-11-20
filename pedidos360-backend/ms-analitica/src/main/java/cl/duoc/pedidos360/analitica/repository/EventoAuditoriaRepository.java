package cl.duoc.pedidos360.analitica.repository;

import cl.duoc.pedidos360.analitica.model.EventoAuditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

    boolean existsByTopicoAndParticionAndOffsetKafka(String topico, int particion, long offsetKafka);

    List<EventoAuditoria> findAllByOrderByIdDesc(Pageable pagina);

    List<EventoAuditoria> findByTopicoOrderByIdDesc(String topico, Pageable pagina);
}
