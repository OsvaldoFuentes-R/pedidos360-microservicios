package cl.duoc.pedidos360.analitica.kafka;

import cl.duoc.pedidos360.analitica.dto.MovimientoInventario;
import cl.duoc.pedidos360.analitica.dto.PedidoEvento;
import cl.duoc.pedidos360.analitica.error.EventoInvalidoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoParserTest {

    private final EventoParser parser = new EventoParser(new ObjectMapper());

    private static final String PEDIDO_OK = """
            {"eventId":"e-1","tipo":"PEDIDO_REGISTRADO","pedidoId":7,"clienteId":1,"clienteNombre":"Camila",
             "total":1500.00,"fecha":"2026-11-10T10:00:00Z",
             "items":[{"productoId":1,"nombreProducto":"Laptop","cantidad":2,"precioUnitario":750.00}],
             "campoNuevo":"se ignora"}""";

    @Test
    void leeUnPedidoValidoIgnorandoCamposDesconocidos() {
        PedidoEvento evento = parser.pedido(PEDIDO_OK);

        assertThat(evento.pedidoId()).isEqualTo(7L);
        assertThat(evento.items()).hasSize(1);
        assertThat(evento.items().get(0).cantidad()).isEqualTo(2);
    }

    @Test
    void rechazaUnMensajeQueNoEsJson() {
        assertThatThrownBy(() -> parser.pedido("esto no es json"))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    void rechazaUnPedidoSinItems() {
        String json = "{\"eventId\":\"e-2\",\"pedidoId\":1,\"total\":10,\"items\":[]}";
        assertThatThrownBy(() -> parser.pedido(json))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("items");
    }

    @Test
    void rechazaUnPedidoSinEventId() {
        assertThatThrownBy(() -> parser.pedido("{\"pedidoId\":1}"))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("eventId");
    }

    @Test
    void leeUnMovimientoDeInventario() {
        MovimientoInventario m = parser.movimiento("""
                {"eventId":"m-1","tipo":"RESERVA","productoId":3,"nombreProducto":"Mouse",
                 "cantidad":1,"stockResultante":9,"fecha":"2026-11-10T10:00:00Z"}""");

        assertThat(m.productoId()).isEqualTo(3L);
        assertThat(m.stockResultante()).isEqualTo(9);
    }

    @Test
    void tipoDeDevuelveDesconocidoSiElJsonEsInvalido() {
        assertThat(parser.tipoDe("{{{")).isEqualTo("DESCONOCIDO");
        assertThat(parser.tipoDe("{\"tipo\":\"RESERVA\"}")).isEqualTo("RESERVA");
    }
}
