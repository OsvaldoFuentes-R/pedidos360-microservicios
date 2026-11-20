package cl.duoc.pedidos360.productos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nombre y parámetros del tópico Kafka que publica este servicio (prefijo "pedidos360.kafka.topics"). */
@ConfigurationProperties(prefix = "pedidos360.kafka.topics")
public record KafkaTopicsProperties(String inventarioMovimientos, int particiones, short replicas, long retencionMs) {
}
