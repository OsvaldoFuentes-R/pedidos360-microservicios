package cl.duoc.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaAdminProperties.class)
public class KafkaAdminConfig {

    /** Cliente de administración de Kafka (se cierra solo al apagar la aplicación). */
    @Bean(destroyMethod = "close")
    public AdminClient adminClient(KafkaAdminProperties props) {
        return AdminClient.create(Map.<String, Object>of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers(),
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, props.timeoutMs(),
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, props.timeoutMs() + 2000));
    }
}
