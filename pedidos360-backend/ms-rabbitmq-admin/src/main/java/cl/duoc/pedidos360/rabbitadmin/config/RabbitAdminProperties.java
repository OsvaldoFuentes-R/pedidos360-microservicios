package cl.duoc.pedidos360.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rabbit-admin")
public record RabbitAdminProperties(String managementUrl, String usuario, String clave, String vhost,
                                    Protegidos protegidos) {

    public record Protegidos(List<String> exchanges, List<String> prefijosColas) {
    }
}
