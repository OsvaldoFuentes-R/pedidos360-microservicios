package cl.duoc.pedidos360.kafkaadmin.service;

import cl.duoc.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import cl.duoc.pedidos360.kafkaadmin.error.KafkaNoDisponibleException;
import cl.duoc.pedidos360.kafkaadmin.error.RecursoNoEncontradoException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Espera el resultado de una operación de Kafka y traduce sus errores a excepciones del servicio. */
@Component
public class KafkaAdminOperaciones {

    private final long timeoutMs;

    public KafkaAdminOperaciones(KafkaAdminProperties props) {
        this.timeoutMs = props.timeoutMs();
    }

    public <T> T esperar(KafkaFuture<T> futuro, String descripcion) {
        try {
            return futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida: " + descripcion, e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondió a tiempo: " + descripcion, e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() == null ? e : e.getCause();
            if (causa instanceof UnknownTopicOrPartitionException || causa instanceof GroupIdNotFoundException) {
                throw new RecursoNoEncontradoException("No existe: " + descripcion);
            }
            if (causa instanceof TimeoutException) {
                throw new KafkaNoDisponibleException("Kafka no respondió a tiempo: " + descripcion, causa);
            }
            if (causa instanceof RuntimeException runtime) {
                throw runtime; // lo traduce el manejador de errores (tópico existente, config inválida, ...)
            }
            throw new KafkaNoDisponibleException("Error de Kafka en: " + descripcion, causa);
        }
    }
}
