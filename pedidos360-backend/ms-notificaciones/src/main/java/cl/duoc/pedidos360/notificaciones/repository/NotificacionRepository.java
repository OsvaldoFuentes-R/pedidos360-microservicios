package cl.duoc.pedidos360.notificaciones.repository;

import cl.duoc.pedidos360.notificaciones.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findAllByOrderByIdDesc();

    List<Notificacion> findByPedidoIdOrderByIdDesc(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);
}
