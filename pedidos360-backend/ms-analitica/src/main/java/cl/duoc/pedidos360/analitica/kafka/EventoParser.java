package cl.duoc.pedidos360.analitica.kafka;

import cl.duoc.pedidos360.analitica.dto.MovimientoInventario;
import cl.duoc.pedidos360.analitica.dto.PedidoEvento;
import cl.duoc.pedidos360.analitica.error.EventoInvalidoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Convierte el JSON de un mensaje en su DTO y valida los campos obligatorios. */
@Component
public class EventoParser {

    private final ObjectMapper mapper;

    public EventoParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PedidoEvento pedido(String json) {
        PedidoEvento evento = leer(json, PedidoEvento.class);
        if (esVacio(evento.eventId())) {
            throw new EventoInvalidoException("el evento de pedido no tiene eventId");
        }
        if (evento.pedidoId() == null) {
            throw new EventoInvalidoException("el evento " + evento.eventId() + " no tiene pedidoId");
        }
        if (evento.total() == null || evento.total().signum() < 0) {
            throw new EventoInvalidoException("el evento " + evento.eventId() + " tiene un total inválido");
        }
        if (evento.items() == null || evento.items().isEmpty()) {
            throw new EventoInvalidoException("el evento " + evento.eventId() + " no trae items");
        }
        return evento;
    }

    public MovimientoInventario movimiento(String json) {
        MovimientoInventario evento = leer(json, MovimientoInventario.class);
        if (esVacio(evento.eventId())) {
            throw new EventoInvalidoException("el movimiento de inventario no tiene eventId");
        }
        if (evento.productoId() == null) {
            throw new EventoInvalidoException("el movimiento " + evento.eventId() + " no tiene productoId");
        }
        if (evento.stockResultante() < 0) {
            throw new EventoInvalidoException("el movimiento " + evento.eventId() + " tiene stock negativo");
        }
        return evento;
    }

    /** Extrae el campo "tipo" para la auditoría; si el JSON es inválido devuelve "DESCONOCIDO". */
    public String tipoDe(String json) {
        try {
            var nodo = mapper.readTree(json).get("tipo");
            return nodo == null ? "DESCONOCIDO" : nodo.asText();
        } catch (JsonProcessingException | RuntimeException e) {
            return "DESCONOCIDO";
        }
    }

    private <T> T leer(String json, Class<T> tipo) {
        if (json == null || json.isBlank()) {
            throw new EventoInvalidoException("el mensaje está vacío");
        }
        try {
            T valor = mapper.readValue(json, tipo);
            if (valor == null) {
                throw new EventoInvalidoException("el mensaje no contiene un objeto JSON");
            }
            return valor;
        } catch (JsonProcessingException e) {
            throw new EventoInvalidoException("el mensaje no es un JSON válido para " + tipo.getSimpleName(), e);
        }
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
