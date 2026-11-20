package cl.duoc.pedidos360.pedidos.client;

import cl.duoc.pedidos360.pedidos.dto.ClienteDto;
import cl.duoc.pedidos360.pedidos.error.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Llamadas a ms-clientes. */
@Component
public class ClientesClient {

    private final RestClient rest;

    public ClientesClient(@Qualifier("clientesRestClient") RestClient rest) {
        this.rest = rest;
    }

    /** @return el cliente, o null si no existe */
    public ClienteDto obtener(Long id) {
        try {
            return rest.get().uri("/clientes/{id}", id).retrieve().body(ClienteDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("ms-clientes no respondió: " + e.getMessage(), e);
        }
    }
}
