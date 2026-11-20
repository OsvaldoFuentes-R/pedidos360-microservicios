package cl.duoc.pedidos360.despacho.repository;

import cl.duoc.pedidos360.despacho.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByOrderByIdDesc();

    Optional<Ticket> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);
}
