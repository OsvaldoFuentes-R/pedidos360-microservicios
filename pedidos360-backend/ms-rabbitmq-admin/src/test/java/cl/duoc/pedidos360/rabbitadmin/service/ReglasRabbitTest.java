package cl.duoc.pedidos360.rabbitadmin.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasRabbitTest {

    @Test
    void aceptaNombresValidos() {
        assertThatCode(() -> ReglasRabbit.validarNombre("pedidos.auditoria")).doesNotThrowAnyException();
        assertThatCode(() -> ReglasRabbit.validarNombre("pedidos360.auditoria:v2")).doesNotThrowAnyException();
    }

    @Test
    void rechazaNombresInvalidosOReservados() {
        assertThatThrownBy(() -> ReglasRabbit.validarNombre("con espacios")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglasRabbit.validarNombre("amq.directo")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglasRabbit.validarNombre(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void protegeLaTopologiaDeLaArquitectura() {
        assertThat(ReglasRabbit.colaProtegida("pedidos.registro", List.of("pedidos."))).isTrue();
        assertThat(ReglasRabbit.colaProtegida("auditoria.log", List.of("pedidos."))).isFalse();
        assertThat(ReglasRabbit.exchangeProtegido("pedidos360.dlx", List.of("pedidos360.dlx"))).isTrue();
    }

    @Test
    void laDeadLetterNecesitaExchangeYRoutingKey() {
        assertThatCode(() -> ReglasRabbit.validarDeadLetter(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> ReglasRabbit.validarDeadLetter("pedidos360.dlx", "dlq.x")).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasRabbit.validarDeadLetter("pedidos360.dlx", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
