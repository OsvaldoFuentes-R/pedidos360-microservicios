package cl.duoc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/** Ejemplo: {"nombre":"pedidos.auditoria","particiones":3,"replicas":3,"configuracion":{"retention.ms":"86400000"}} */
public record TopicoRequest(
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @Min(value = 1, message = "las particiones deben ser al menos 1") int particiones,
        @Min(value = 1, message = "las réplicas deben ser al menos 1") short replicas,
        Map<String, String> configuracion) {
}
