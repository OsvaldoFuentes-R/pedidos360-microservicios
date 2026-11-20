package cl.duoc.pedidos360.analitica.config;

import cl.duoc.pedidos360.analitica.error.EventoInvalidoException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Manejo de errores de los consumidores:
 *  - un error temporal (por ejemplo, la BD no responde) se reintenta {@code maximo} veces cada {@code intervaloMs};
 *  - un mensaje inválido ({@link EventoInvalidoException}) no se reintenta;
 *  - si se agotan los reintentos, el mensaje se publica en el tópico "&lt;topico&gt;.dlt" (Dead Letter Topic)
 *    en la misma partición y el consumidor sigue con el siguiente. Así un mensaje defectuoso no bloquea el flujo.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> plantilla, KafkaTopicsProperties props) {
        DeadLetterPublishingRecoverer recuperador = new DeadLetterPublishingRecoverer(plantilla,
                (registro, error) -> new TopicPartition(props.topics().dltDe(registro.topic()), registro.partition()));
        DefaultErrorHandler manejador = new DefaultErrorHandler(recuperador,
                new FixedBackOff(props.reintentos().intervaloMs(), props.reintentos().maximo()));
        manejador.addNotRetryableExceptions(EventoInvalidoException.class);
        return manejador;
    }

    @Bean
    public NewTopic pedidosEventosDlt(KafkaTopicsProperties props) {
        return dlt(props, props.topics().pedidosEventos());
    }

    @Bean
    public NewTopic inventarioMovimientosDlt(KafkaTopicsProperties props) {
        return dlt(props, props.topics().inventarioMovimientos());
    }

    private NewTopic dlt(KafkaTopicsProperties props, String topico) {
        return TopicBuilder.name(props.topics().dltDe(topico))
                .partitions(props.topics().particiones())
                .replicas(props.topics().replicas())
                .config("min.insync.replicas", "2")
                .build();
    }
}
