package cl.duoc.pedidos360.analitica.service;

import cl.duoc.pedidos360.analitica.dto.KpisResumen;
import cl.duoc.pedidos360.analitica.dto.PedidoEvento;
import cl.duoc.pedidos360.analitica.model.PedidoAnalitica;
import cl.duoc.pedidos360.analitica.model.VentaProducto;
import cl.duoc.pedidos360.analitica.repository.PedidoAnaliticaRepository;
import cl.duoc.pedidos360.analitica.repository.VentaProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaliticaPedidosServiceTest {

    @Mock
    private PedidoAnaliticaRepository pedidos;
    @Mock
    private VentaProductoRepository ventas;

    @InjectMocks
    private AnaliticaPedidosService servicio;

    private PedidoEvento evento() {
        return new PedidoEvento("e-1", "PEDIDO_REGISTRADO", 7L, 1L, "Camila", new BigDecimal("1500.00"),
                "2026-11-10T10:00:00Z",
                List.of(new PedidoEvento.ItemEvento(1L, "Laptop", 2, new BigDecimal("750.00")),
                        new PedidoEvento.ItemEvento(2L, "Mouse", 1, new BigDecimal("0.00"))));
    }

    @Test
    void contabilizaElPedidoYSumaLasUnidadesPorProducto() {
        when(pedidos.existsByEventId("e-1")).thenReturn(false);
        when(ventas.findById(1L)).thenReturn(Optional.empty());
        when(ventas.findById(2L)).thenReturn(Optional.of(new VentaProducto(2L, "Mouse")));

        boolean aplicado = servicio.procesar(evento());

        assertThat(aplicado).isTrue();
        verify(pedidos).save(any(PedidoAnalitica.class));
        verify(ventas, org.mockito.Mockito.times(2)).save(any(VentaProducto.class));
    }

    @Test
    void ignoraUnEventoRepetido() {
        when(pedidos.existsByEventId("e-1")).thenReturn(true);

        assertThat(servicio.procesar(evento())).isFalse();
        verify(pedidos, never()).save(any(PedidoAnalitica.class));
    }

    @Test
    void kpisCalculaElTicketPromedio() {
        when(pedidos.count()).thenReturn(4L);
        when(pedidos.sumaIngresos()).thenReturn(new BigDecimal("1000.00"));
        when(pedidos.sumaUnidades()).thenReturn(9L);

        KpisResumen kpis = servicio.kpis();

        assertThat(kpis.ticketPromedio()).isEqualByComparingTo("250.00");
        assertThat(kpis.unidadesVendidas()).isEqualTo(9L);
    }

    @Test
    void kpisSinPedidosDevuelveCeros() {
        when(pedidos.count()).thenReturn(0L);
        when(pedidos.sumaIngresos()).thenReturn(BigDecimal.ZERO);
        when(pedidos.sumaUnidades()).thenReturn(0L);

        assertThat(servicio.kpis().ticketPromedio()).isEqualByComparingTo("0");
    }
}
