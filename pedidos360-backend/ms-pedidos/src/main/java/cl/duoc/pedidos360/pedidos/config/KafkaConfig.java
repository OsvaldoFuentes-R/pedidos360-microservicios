package cl.duoc.pedidos360.pedidos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara el tópico pedidos.eventos. Spring lo crea al iniciar si no existe; si ya existe
 * (lo creó infra/kafka/crear-topicos.sh o ms-kafka-admin) no se toca.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfig {

    @Bean
    public NewTopic pedidosEventosTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.pedidosEventos())
                .partitions(topics.particiones())
                .replicas(topics.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(topics.retencionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}
