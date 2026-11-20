package cl.duoc.pedidos360.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/** Parámetros del servicio (prefijo "kafka" del application.yml). */
@ConfigurationProperties(prefix = "kafka")
public record KafkaAdminProperties(String bootstrapServers, int timeoutMs, Admin admin) {

    public record Admin(boolean crearAlIniciar, List<String> protegidos, List<TopicoInicial> topicos) {
    }

    public record TopicoInicial(String nombre, int particiones, short replicas, Map<String, String> configuracion) {
    }
}
