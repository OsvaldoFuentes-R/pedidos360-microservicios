package cl.duoc.pedidos360.clientes.service;

import cl.duoc.pedidos360.clientes.error.ConflictoException;
import cl.duoc.pedidos360.clientes.error.RecursoNoEncontradoException;
import cl.duoc.pedidos360.clientes.model.Cliente;
import cl.duoc.pedidos360.clientes.repository.ClienteRepository;
import cl.duoc.pedidos360.clientes.web.ClienteRequest;
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
class ClienteServiceTest {

    @Mock
    private ClienteRepository repositorio;

    @InjectMocks
    private ClienteService servicio;

    private ClienteRequest solicitud() {
        return new ClienteRequest("Ana Pérez", "ana@example.com", "+56 9 0000 0000", "Calle 1, Santiago");
    }

    @Test
    void crearGuardaElClienteCuandoElEmailEsNuevo() {
        when(repositorio.existsByEmail("ana@example.com")).thenReturn(false);
        when(repositorio.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cliente creado = servicio.crear(solicitud());

        assertThat(creado.getEmail()).isEqualTo("ana@example.com");
        assertThat(creado.getNombre()).isEqualTo("Ana Pérez");
    }

    @Test
    void crearRechazaUnEmailRepetido() {
        when(repositorio.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(solicitud()))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ana@example.com");
        verify(repositorio, never()).save(any(Cliente.class));
    }

    @Test
    void obtenerFallaSiElClienteNoExiste() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
