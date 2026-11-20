package cl.duoc.pedidos360.pedidos.client;

import cl.duoc.pedidos360.pedidos.dto.CantidadRequest;
import cl.duoc.pedidos360.pedidos.dto.ProductoDto;
import cl.duoc.pedidos360.pedidos.error.ServicioNoDisponibleException;
import cl.duoc.pedidos360.pedidos.error.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Llamadas a ms-productos. */
@Component
public class ProductosClient {

    private final RestClient rest;

    public ProductosClient(@Qualifier("productosRestClient") RestClient rest) {
        this.rest = rest;
    }

    /** @return el producto, o null si no existe */
    public ProductoDto obtener(Long id) {
        try {
            return rest.get().uri("/productos/{id}", id).retrieve().body(ProductoDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("ms-productos no respondió: " + e.getMessage(), e);
        }
    }

    /** Descuenta stock. Lanza SolicitudInvalidaException si ms-productos responde 409 (sin stock). */
    public void reservar(Long id, int cantidad) {
        try {
            rest.post().uri("/productos/{id}/reservar", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CantidadRequest(cantidad))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new SolicitudInvalidaException("Stock insuficiente del producto " + id
                    + " (cantidad solicitada: " + cantidad + ")");
        } catch (HttpClientErrorException.NotFound e) {
            throw new SolicitudInvalidaException("El producto " + id + " no existe");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("ms-productos no respondió: " + e.getMessage(), e);
        }
    }

    /** Devuelve stock reservado (compensación). */
    public void liberar(Long id, int cantidad) {
        try {
            rest.post().uri("/productos/{id}/liberar", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CantidadRequest(cantidad))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo liberar el stock del producto " + id
                    + ": " + e.getMessage(), e);
        }
    }
}
