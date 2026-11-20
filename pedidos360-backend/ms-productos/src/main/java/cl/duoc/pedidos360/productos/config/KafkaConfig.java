package cl.duoc.pedidos360.productos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declara el tópico inventario.movimientos (se crea al iniciar si no existe). */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfig {

    @Bean
    public NewTopic inventarioMovimientosTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.inventarioMovimientos())
                .partitions(topics.particiones())
                .replicas(topics.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(topics.retencionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}
