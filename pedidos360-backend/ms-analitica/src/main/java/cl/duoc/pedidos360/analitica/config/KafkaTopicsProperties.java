package cl.duoc.pedidos360.analitica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tópicos y consumer groups (prefijo "pedidos360.kafka"). Los nombres solo viven en application.yml. */
@ConfigurationProperties(prefix = "pedidos360.kafka")
public record KafkaTopicsProperties(Topics topics, Grupos grupos, Reintentos reintentos) {

    public record Topics(String pedidosEventos, String inventarioMovimientos, String sufijoDlt,
                         int particiones, short replicas) {

        public String dltDe(String topico) {
            return topico + sufijoDlt;
        }
    }

    public record Grupos(String kpis, String inventario, String auditoria) {
    }

    public record Reintentos(long intervaloMs, long maximo) {
    }
}
