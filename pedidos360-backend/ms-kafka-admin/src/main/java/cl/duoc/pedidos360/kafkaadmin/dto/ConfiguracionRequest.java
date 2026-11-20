package cl.duoc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Ejemplo: {"configuracion":{"retention.ms":"172800000","cleanup.policy":"delete"}} */
public record ConfiguracionRequest(
        @NotEmpty(message = "indica al menos una configuración") Map<String, String> configuracion) {
}
