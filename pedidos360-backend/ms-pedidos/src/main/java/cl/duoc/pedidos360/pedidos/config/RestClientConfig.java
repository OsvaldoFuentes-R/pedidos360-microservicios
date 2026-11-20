package cl.duoc.pedidos360.pedidos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Clientes HTTP hacia ms-productos y ms-clientes, con tiempos máximos de espera. */
@Configuration
@EnableConfigurationProperties(ServiciosProperties.class)
public class RestClientConfig {

    @Bean("productosRestClient")
    public RestClient productosRestClient(ServiciosProperties servicios) {
        return crear(servicios.productosUrl());
    }

    @Bean("clientesRestClient")
    public RestClient clientesRestClient(ServiciosProperties servicios) {
        return crear(servicios.clientesUrl());
    }

    private RestClient crear(String urlBase) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(2000);
        fabrica.setReadTimeout(5000);
        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .build();
    }
}
