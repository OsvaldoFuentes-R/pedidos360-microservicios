package cl.duoc.pedidos360.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Direcciones de los microservicios que consulta el BFF (prefijo "servicios"). */
@ConfigurationProperties(prefix = "servicios")
public record ServiciosProperties(String pedidosUrl, String productosUrl, String clientesUrl, String despachoUrl, String analiticaUrl) {
}
