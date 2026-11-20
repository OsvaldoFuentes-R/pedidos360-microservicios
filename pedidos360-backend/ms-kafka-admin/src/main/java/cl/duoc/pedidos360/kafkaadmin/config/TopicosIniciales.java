package cl.duoc.pedidos360.kafkaadmin.config;

import cl.duoc.pedidos360.kafkaadmin.service.TopicoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al iniciar, deja creados los tópicos definidos en application.yml (3 particiones, 3 réplicas). */
@Component
public class TopicosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopicosIniciales.class);

    private final TopicoService topicos;
    private final KafkaAdminProperties props;

    public TopicosIniciales(TopicoService topicos, KafkaAdminProperties props) {
        this.topicos = topicos;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.admin().crearAlIniciar()) {
            return;
        }
        for (KafkaAdminProperties.TopicoInicial def : props.admin().topicos()) {
            try {
                topicos.crearSiNoExiste(def);
            } catch (RuntimeException e) {
                // Kafka puede no estar listo todavía: el servicio sigue arriba y se pueden crear por la API
                log.warn("No se pudo crear el tópico {} al iniciar: {}", def.nombre(), e.getMessage());
            }
        }
    }
}
