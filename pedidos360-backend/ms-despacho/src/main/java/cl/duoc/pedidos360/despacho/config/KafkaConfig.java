package cl.duoc.pedidos360.despacho.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** El tópico pedidos.eventos lo crea ms-pedidos / infra/kafka / ms-kafka-admin; aquí solo se lee su nombre. */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfig {
}
