package cl.duoc.pedidos360.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Direcciones de los microservicios que consulta ms-pedidos (prefijo "servicios"). */
@ConfigurationProperties(prefix = "servicios")
public record ServiciosProperties(String productosUrl, String clientesUrl) {
}
