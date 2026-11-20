package cl.duoc.pedidos360.despacho.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tópico Kafka en el que publica este servicio (prefijo "pedidos360.kafka.topics"). */
@ConfigurationProperties(prefix = "pedidos360.kafka.topics")
public record KafkaTopicsProperties(String pedidosEventos) {
}
