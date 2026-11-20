package cl.duoc.pedidos360.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BindingInfo(String source, String destination, String destination_type, String routing_key) {
}
