package cl.duoc.pedidos360.despacho.service;

import cl.duoc.pedidos360.despacho.dto.EventoTicket;
import cl.duoc.pedidos360.despacho.error.ConflictoException;
import cl.duoc.pedidos360.despacho.messaging.TicketKafkaPublisher;
import cl.duoc.pedidos360.despacho.model.EstadoTicket;
import cl.duoc.pedidos360.despacho.model.Ticket;
import cl.duoc.pedidos360.despacho.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository repositorio;

    @Mock
    private TicketKafkaPublisher kafka;

    @InjectMocks
    private TicketService servicio;

    private EventoTicket evento(String direccion) {
        return new EventoTicket(7L, "Camila Rojas", direccion, 3);
    }

    @Test
    void generarCreaElTicketConCodigoYEstadoInicial() {
        when(repositorio.existsByPedidoId(7L)).thenReturn(false);
        when(repositorio.save(any(Ticket.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Ticket ticket = servicio.generar(evento("Av. Providencia 1234"));

        assertThat(ticket.getCodigo()).isEqualTo("TCK-000007");
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.GENERADO);
        assertThat(ticket.getTotalItems()).isEqualTo(3);
    }

    @Test
    void generarIgnoraUnEventoRepetido() {
        when(repositorio.existsByPedidoId(7L)).thenReturn(true);

        assertThat(servicio.generar(evento("Av. Providencia 1234"))).isNull();
        verify(repositorio, never()).save(any(Ticket.class));
    }

    @Test
    void generarRechazaUnEventoSinDireccion() {
        assertThatThrownBy(() -> servicio.generar(evento("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dirección");
    }

    @Test
    void cambiarEstadoAvanzaUnPasoALaVez() {
        Ticket ticket = new Ticket(7L, "TCK-000007", "Camila Rojas", "Av. 1", 3);
        when(repositorio.findById(1L)).thenReturn(Optional.of(ticket));
        when(repositorio.save(any(Ticket.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Ticket actualizado = servicio.cambiarEstado(1L, EstadoTicket.EN_RUTA);

        assertThat(actualizado.getEstado()).isEqualTo(EstadoTicket.EN_RUTA);
        assertThat(actualizado.getFechaActualizacion()).isNotNull();
        verify(kafka).publicarCambioEstado(actualizado, EstadoTicket.GENERADO);
    }

    @Test
    void cambiarEstadoRechazaSaltarPasos() {
        Ticket ticket = new Ticket(7L, "TCK-000007", "Camila Rojas", "Av. 1", 3);
        when(repositorio.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, EstadoTicket.ENTREGADO))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("GENERADO");
        verify(repositorio, never()).save(any(Ticket.class));
        verify(kafka, never()).publicarCambioEstado(any(Ticket.class), any(EstadoTicket.class));
    }
}
