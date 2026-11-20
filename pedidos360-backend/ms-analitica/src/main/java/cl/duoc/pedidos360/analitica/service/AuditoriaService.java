package cl.duoc.pedidos360.analitica.service;

import cl.duoc.pedidos360.analitica.model.EventoAuditoria;
import cl.duoc.pedidos360.analitica.repository.EventoAuditoriaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Línea de tiempo de auditoría: guarda cada mensaje recibido tal cual, con su partición y offset. */
@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repositorio;

    public AuditoriaService(EventoAuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public boolean registrar(String topico, int particion, long offset, String clave, String tipo, String payload) {
        if (repositorio.existsByTopicoAndParticionAndOffsetKafka(topico, particion, offset)) {
            return false;
        }
        repositorio.save(new EventoAuditoria(topico, particion, offset, clave, tipo, payload));
        return true;
    }

    @Transactional(readOnly = true)
    public List<EventoAuditoria> ultimos(String topico, int limite) {
        PageRequest pagina = PageRequest.of(0, Math.max(1, Math.min(limite, 200)));
        return topico == null || topico.isBlank()
                ? repositorio.findAllByOrderByIdDesc(pagina)
                : repositorio.findByTopicoOrderByIdDesc(topico, pagina);
    }
}
