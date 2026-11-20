package cl.duoc.pedidos360.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** Cola tal como la informa la API de administración de RabbitMQ. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ColaInfo(String name, Boolean durable, Integer messages, Integer consumers, String state,
                       String node, Map<String, Object> arguments) {
}
