package cl.duoc.pedidos360.notificaciones.service;

import cl.duoc.pedidos360.notificaciones.dto.EventoNotificacion;
import cl.duoc.pedidos360.notificaciones.model.Notificacion;
import cl.duoc.pedidos360.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repositorio;

    @InjectMocks
    private NotificacionService servicio;

    private EventoNotificacion evento(String email, String mensaje) {
        return new EventoNotificacion(5L, 1L, "Camila", email, new BigDecimal("1000.00"), mensaje);
    }

    @Test
    void registrarGuardaLaNotificacion() {
        when(repositorio.existsByPedidoId(5L)).thenReturn(false);
        when(repositorio.save(any(Notificacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Notificacion resultado = servicio.registrar(evento("camila@example.com", "Tu pedido #5 fue registrado"));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDestinatario()).isEqualTo("camila@example.com");
        assertThat(resultado.getPedidoId()).isEqualTo(5L);
        assertThat(resultado.getEstado()).isEqualTo("ENVIADA");
    }

    @Test
    void registrarIgnoraUnEventoRepetido() {
        when(repositorio.existsByPedidoId(5L)).thenReturn(true);

        Notificacion resultado = servicio.registrar(evento("camila@example.com", "Tu pedido #5 fue registrado"));

        assertThat(resultado).isNull();
        verify(repositorio, never()).save(any(Notificacion.class));
    }

    @Test
    void registrarRechazaUnEventoSinEmail() {
        assertThatThrownBy(() -> servicio.registrar(evento(" ", "Hola")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
        verify(repositorio, never()).save(any(Notificacion.class));
    }

    @Test
    void registrarRechazaUnEventoSinPedidoId() {
        EventoNotificacion sinPedido = new EventoNotificacion(null, 1L, "Camila", "c@example.com",
                BigDecimal.ONE, "Hola");

        assertThatThrownBy(() -> servicio.registrar(sinPedido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pedidoId");
    }
}
