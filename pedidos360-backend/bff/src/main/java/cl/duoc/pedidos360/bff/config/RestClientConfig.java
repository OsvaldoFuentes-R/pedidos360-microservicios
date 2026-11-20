package cl.duoc.pedidos360.bff.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Un RestClient por microservicio, con tiempos máximos de espera. */
@Configuration
@EnableConfigurationProperties(ServiciosProperties.class)
public class RestClientConfig {

    @Bean("pedidosRestClient")
    public RestClient pedidosRestClient(ServiciosProperties s) {
        return crear(s.pedidosUrl());
    }

    @Bean("productosRestClient")
    public RestClient productosRestClient(ServiciosProperties s) {
        return crear(s.productosUrl());
    }

    @Bean("clientesRestClient")
    public RestClient clientesRestClient(ServiciosProperties s) {
        return crear(s.clientesUrl());
    }

    @Bean("despachoRestClient")
    public RestClient despachoRestClient(ServiciosProperties s) {
        return crear(s.despachoUrl());
    }

    @Bean("analiticaRestClient")
    public RestClient analiticaRestClient(ServiciosProperties s) {
        return crear(s.analiticaUrl());
    }

    private RestClient crear(String urlBase) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(2000);
        fabrica.setReadTimeout(5000);
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }
}
