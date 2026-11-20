package cl.duoc.pedidos360.pedidos.service;

import cl.duoc.pedidos360.pedidos.client.ClientesClient;
import cl.duoc.pedidos360.pedidos.client.ProductosClient;
import cl.duoc.pedidos360.pedidos.dto.ClienteDto;
import cl.duoc.pedidos360.pedidos.dto.EventoNotificacion;
import cl.duoc.pedidos360.pedidos.dto.EventoTicket;
import cl.duoc.pedidos360.pedidos.dto.ProductoDto;
import cl.duoc.pedidos360.pedidos.dto.SolicitudPedido;
import cl.duoc.pedidos360.pedidos.dto.SolicitudPedido.ItemSolicitud;
import cl.duoc.pedidos360.pedidos.error.SolicitudInvalidaException;
import cl.duoc.pedidos360.pedidos.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.pedidos.messaging.PedidoKafkaPublisher;
import cl.duoc.pedidos360.pedidos.model.Pedido;
import cl.duoc.pedidos360.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repositorio;
    @Mock
    private ClientesClient clientes;
    @Mock
    private ProductosClient productos;
    @Mock
    private PedidoEventPublisher publicador;
    @Mock
    private PedidoKafkaPublisher kafkaPublicador;

    @InjectMocks
    private PedidoService servicio;

    private ClienteDto cliente() {
        return new ClienteDto(1L, "Camila Rojas", "camila@example.com", "Av. Providencia 1234");
    }

    private ProductoDto producto(Long id, String nombre, String precio, int stock) {
        return new ProductoDto(id, nombre, new BigDecimal(precio), stock);
    }

    @Test
    void registrarGuardaElPedidoReservaStockYPublicaLosEventos() {
        when(clientes.obtener(1L)).thenReturn(cliente());
        when(productos.obtener(10L)).thenReturn(producto(10L, "Mouse", "1000.00", 5));
        when(repositorio.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Pedido pedido = servicio.registrar(new SolicitudPedido(1L, List.of(new ItemSolicitud(10L, 2))));

        assertThat(pedido.getTotal()).isEqualByComparingTo("2000.00");
        assertThat(pedido.getItems()).hasSize(1);
        assertThat(pedido.getClienteNombre()).isEqualTo("Camila Rojas");
        verify(productos).reservar(10L, 2);
        verify(publicador).publicarNotificacion(any(EventoNotificacion.class));
        verify(publicador).publicarTicket(any(EventoTicket.class));
    }

    @Test
    void registrarRechazaUnClienteInexistente() {
        when(clientes.obtener(99L)).thenReturn(null);

        assertThatThrownBy(() -> servicio.registrar(
                new SolicitudPedido(99L, List.of(new ItemSolicitud(10L, 1)))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("99");
        verifyNoInteractions(productos, publicador);
    }

    @Test
    void registrarRechazaCuandoNoAlcanzaElStockYNoReservaNada() {
        when(clientes.obtener(1L)).thenReturn(cliente());
        when(productos.obtener(10L)).thenReturn(producto(10L, "Mouse", "1000.00", 1));

        assertThatThrownBy(() -> servicio.registrar(
                new SolicitudPedido(1L, List.of(new ItemSolicitud(10L, 5)))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(productos, never()).reservar(anyLong(), anyInt());
        verifyNoInteractions(publicador);
    }

    @Test
    void registrarDevuelveElStockYaReservadoSiFallaUnaReservaPosterior() {
        when(clientes.obtener(1L)).thenReturn(cliente());
        when(productos.obtener(10L)).thenReturn(producto(10L, "Mouse", "1000.00", 5));
        when(productos.obtener(11L)).thenReturn(producto(11L, "Teclado", "2000.00", 5));
        doNothing().when(productos).reservar(10L, 1);
        doThrow(new SolicitudInvalidaException("Stock insuficiente del producto 11"))
                .when(productos).reservar(11L, 1);

        assertThatThrownBy(() -> servicio.registrar(new SolicitudPedido(1L,
                List.of(new ItemSolicitud(10L, 1), new ItemSolicitud(11L, 1)))))
                .isInstanceOf(SolicitudInvalidaException.class);

        verify(productos).liberar(10L, 1);
        verify(productos, never()).liberar(eq(11L), anyInt());
        verify(repositorio, never()).save(any(Pedido.class));
        verifyNoInteractions(publicador);
    }

    @Test
    void registrarRechazaUnaSolicitudSinProductos() {
        assertThatThrownBy(() -> servicio.registrar(new SolicitudPedido(1L, List.of())))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("no tiene productos");
        verifyNoInteractions(clientes, productos, publicador);
    }

    @Test
    void registrarRechazaUnaCantidadNoPositiva() {
        assertThatThrownBy(() -> servicio.registrar(
                new SolicitudPedido(1L, List.of(new ItemSolicitud(10L, 0)))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("mayor que cero");
        verifyNoInteractions(clientes, productos, publicador);
    }
}