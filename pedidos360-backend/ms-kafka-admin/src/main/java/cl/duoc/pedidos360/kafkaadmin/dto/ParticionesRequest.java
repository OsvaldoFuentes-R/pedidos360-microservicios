package cl.duoc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Min;

/** Kafka solo permite AUMENTAR las particiones, nunca reducirlas. */
public record ParticionesRequest(@Min(value = 1, message = "las particiones deben ser al menos 1") int particiones) {
}
