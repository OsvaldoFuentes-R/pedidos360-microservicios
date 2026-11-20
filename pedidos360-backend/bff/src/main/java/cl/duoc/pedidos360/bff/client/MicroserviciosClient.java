package cl.duoc.pedidos360.bff.client;

import cl.duoc.pedidos360.bff.dto.PedidoRemoto;
import cl.duoc.pedidos360.bff.dto.TicketRemoto;
import cl.duoc.pedidos360.bff.error.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Llamadas HTTP del BFF a los microservicios. Cualquier falla de red se convierte en ServicioNoDisponibleException. */
@Component
public class MicroserviciosClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA_GENERICA =
            new ParameterizedTypeReference<>() { };

    private final RestClient pedidos;
    private final RestClient productos;
    private final RestClient clientes;
    private final RestClient despacho;
    private final RestClient analitica;

    public MicroserviciosClient(@Qualifier("pedidosRestClient") RestClient pedidos,
                                @Qualifier("productosRestClient") RestClient productos,
                                @Qualifier("clientesRestClient") RestClient clientes,
                                @Qualifier("despachoRestClient") RestClient despacho,
                                @Qualifier("analiticaRestClient") RestClient analitica) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.clientes = clientes;
        this.despacho = despacho;
        this.analitica = analitica;
    }

    public List<PedidoRemoto> listarPedidos() {
        return llamar("ms-pedidos", () -> pedidos.get().uri("/pedidos").retrieve()
                .body(new ParameterizedTypeReference<List<PedidoRemoto>>() { }));
    }

    public PedidoRemoto obtenerPedido(Long id) {
        return llamar("ms-pedidos", () -> pedidos.get().uri("/pedidos/{id}", id).retrieve()
                .body(PedidoRemoto.class));
    }

    public List<TicketRemoto> listarTickets() {
        return llamar("ms-despacho", () -> despacho.get().uri("/tickets").retrieve()
                .body(new ParameterizedTypeReference<List<TicketRemoto>>() { }));
    }

    public TicketRemoto ticketDePedido(Long pedidoId) {
        try {
            return llamar("ms-despacho", () -> despacho.get().uri("/tickets/pedido/{id}", pedidoId).retrieve()
                    .body(TicketRemoto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return null; // el pedido todavía no tiene ticket
        }
    }

    public List<Map<String, Object>> listarProductos() {
        return llamar("ms-productos", () -> productos.get().uri("/productos").retrieve().body(LISTA_GENERICA));
    }

    public List<Map<String, Object>> listarClientes() {
        return llamar("ms-clientes", () -> clientes.get().uri("/clientes").retrieve().body(LISTA_GENERICA));
    }

    /** Consulta de solo lectura a ms-analitica (KPIs, top de productos, stock crítico, auditoría). */
    public Object analitica(String ruta, Map<String, Object> parametros) {
        return llamar("ms-analitica", () -> analitica.get().uri(uri -> {
            uri.path(ruta);
            parametros.forEach((nombre, valor) -> {
                if (valor != null) {
                    uri.queryParam(nombre, valor);
                }
            });
            return uri.build();
        }).retrieve().body(Object.class));
    }

    private <T> T llamar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException e) {
            throw e; // 4xx: lo resuelve el manejador de errores
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("El servicio " + servicio + " no está disponible", e);
        }
    }
}
