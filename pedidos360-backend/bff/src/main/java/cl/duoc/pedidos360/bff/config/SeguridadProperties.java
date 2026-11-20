package cl.duoc.pedidos360.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parámetros de seguridad. issuer y audience son opcionales: si se definen, el token debe venir
 * emitido por ese issuer y dirigido a esa audiencia (el client id de la API en Azure).
 */
@ConfigurationProperties(prefix = "seguridad")
public record SeguridadProperties(String jwkSetUri, String issuer, String audience, List<String> corsOrigenes) {
}
