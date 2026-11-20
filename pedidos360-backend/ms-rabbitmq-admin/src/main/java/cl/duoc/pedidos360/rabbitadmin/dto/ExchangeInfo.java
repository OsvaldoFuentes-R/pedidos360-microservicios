package cl.duoc.pedidos360.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeInfo(String name, String type, Boolean durable, Map<String, Object> arguments) {
}
