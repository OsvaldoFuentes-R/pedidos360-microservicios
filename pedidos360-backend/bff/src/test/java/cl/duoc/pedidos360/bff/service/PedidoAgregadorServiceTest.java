package cl.duoc.pedidos360.bff.service;

import cl.duoc.pedidos360.bff.client.MicroserviciosClient;
import cl.duoc.pedidos360.bff.dto.PedidoRemoto;
import cl.duoc.pedidos360.bff.dto.PedidoVista;
import cl.duoc.pedidos360.bff.dto.TicketRemoto;
import cl.duoc.pedidos360.bff.error.ServicioNoDisponibleException;
import cl.duoc.pedidos360.bff.messaging.RegistroPedidoPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoAgregadorServiceTest {

    @Mock
    private MicroserviciosClient microservicios;
    @Mock
    private RegistroPedidoPublisher publisher;

    @InjectMocks
    private PedidoAgregadorService servicio;

    private PedidoRemoto pedido(Long id, String estado) {
        return new PedidoRemoto(id, 1L, "Camila Rojas", new BigDecimal("2000.00"), estado, LocalDateTime.now(),
                List.of(new PedidoRemoto.ItemRemoto(1L, "Laptop", 2, new BigDecimal("1000.00")),
                        new PedidoRemoto.ItemRemoto(2L, "Mouse", 1, new BigDecimal("10.00"))));
    }

    @Test
    void elEstadoSaleDelTicketDeDespacho() {
        when(microservicios.listarPedidos()).thenReturn(List.of(pedido(1L, "REGISTRADO"), pedido(2L, "REGISTRADO"),
                pedido(3L, "REGISTRADO")));
        when(microservicios.listarTickets()).thenReturn(List.of(
                new TicketRemoto(1L, 1L, "TCK-000001", "EN_RUTA"),
                new TicketRemoto(2L, 2L, "TCK-000002", "ENTREGADO")));

        List<PedidoVista> vistas = servicio.listar();

        assertThat(vistas).extracting(PedidoVista::estado)
                .containsExactly("En tránsito", "Entregado", "Procesando");
        assertThat(vistas.get(0).producto()).isEqualTo("Laptop x2, Mouse");
        assertThat(vistas.get(0).ticket()).isEqualTo("TCK-000001");
        assertThat(vistas.get(2).ticket()).isNull();
    }

    @Test
    void siDespachoNoResponderLosPedidosSeMuestranComoProcesando() {
        when(microservicios.listarPedidos()).thenReturn(List.of(pedido(1L, "REGISTRADO")));
        when(microservicios.listarTickets()).thenThrow(new ServicioNoDisponibleException("caído", null));

        List<PedidoVista> vistas = servicio.listar();

        assertThat(vistas).hasSize(1);
        assertThat(vistas.get(0).estado()).isEqualTo("Procesando");
    }

    @Test
    void unPedidoCanceladoSeMuestraCancelado() {
        assertThat(servicio.estadoVisible("CANCELADO", null)).isEqualTo("Cancelado");
    }
}
