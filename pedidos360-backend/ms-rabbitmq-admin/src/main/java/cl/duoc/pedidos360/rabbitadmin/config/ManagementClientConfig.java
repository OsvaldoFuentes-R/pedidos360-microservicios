package cl.duoc.pedidos360.rabbitadmin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Cliente HTTP hacia la API de administración de RabbitMQ (autenticación básica). */
@Configuration
@EnableConfigurationProperties(RabbitAdminProperties.class)
public class ManagementClientConfig {

    @Bean
    public RestClient managementRestClient(RabbitAdminProperties props) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(2000);
        fabrica.setReadTimeout(5000);
        String credenciales = props.usuario() + ":" + props.clave();
        String autorizacion = "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .baseUrl(props.managementUrl())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.AUTHORIZATION, autorizacion)
                .build();
    }
}
