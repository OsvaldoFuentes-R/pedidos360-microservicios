package cl.duoc.pedidos360.productos.service;

import cl.duoc.pedidos360.productos.error.RecursoNoEncontradoException;
import cl.duoc.pedidos360.productos.error.StockInsuficienteException;
import cl.duoc.pedidos360.productos.messaging.InventarioKafkaPublisher;
import cl.duoc.pedidos360.productos.model.Producto;
import cl.duoc.pedidos360.productos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repositorio;
    @Mock
    private InventarioKafkaPublisher kafka;

    @InjectMocks
    private ProductoService servicio;

    private Producto producto(int stock) {
        return new Producto("Mouse", "Mouse de prueba", new BigDecimal("1000.00"), stock, "Accesorios");
    }

    @Test
    void reservarDescuentaStockCuandoHayDisponible() {
        when(repositorio.descontarStock(1L, 2)).thenReturn(1);
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(8)));

        Producto resultado = servicio.reservar(1L, 2);

        assertThat(resultado.getNombre()).isEqualTo("Mouse");
        verify(repositorio).descontarStock(1L, 2);
    }

    @Test
    void reservarLanzaConflictoSiNoAlcanzaElStock() {
        when(repositorio.descontarStock(1L, 50)).thenReturn(0);
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(3)));

        assertThatThrownBy(() -> servicio.reservar(1L, 50))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible 3");
    }

    @Test
    void reservarLanzaNoEncontradoSiElProductoNoExiste() {
        when(repositorio.descontarStock(99L, 1)).thenReturn(0);
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.reservar(99L, 1))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void liberarFallaSiElProductoNoExiste() {
        when(repositorio.reponerStock(99L, 1)).thenReturn(0);

        assertThatThrownBy(() -> servicio.liberar(99L, 1))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
