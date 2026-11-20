package cl.duoc.pedidos360.kafkaadmin.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class ReglasTopicoTest {

    @Test
    void aceptaNombresValidos() {
        assertThatCode(() -> ReglasTopico.validarNombre("pedidos.eventos")).doesNotThrowAnyException();
        assertThatCode(() -> ReglasTopico.validarNombre("inventario_eventos-v2")).doesNotThrowAnyException();
    }

    @Test
    void rechazaNombresInvalidos() {
        assertThatThrownBy(() -> ReglasTopico.validarNombre("con espacios")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglasTopico.validarNombre("..")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglasTopico.validarNombre("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglasTopico.validarNombre(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaConfiguracionesNoPermitidas() {
        assertThatThrownBy(() -> ReglasTopico.validarConfiguracion(Map.of("unclean.leader.election.enable", "true")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está permitida");
    }

    @Test
    void validaLaPoliticaDeLimpieza() {
        assertThatCode(() -> ReglasTopico.validarConfiguracion(Map.of("cleanup.policy", "compact,delete")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasTopico.validarConfiguracion(Map.of("cleanup.policy", "borrar")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lasReplicasNoPuedenSuperarLosBrokers() {
        assertThatCode(() -> ReglasTopico.validarReplicas(3, 3)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasTopico.validarReplicas(3, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 brokers");
    }

    @Test
    void detectaTopicosProtegidos() {
        List<String> protegidos = List.of("pedidos.eventos", "inventario.eventos");
        assertThat(ReglasTopico.esProtegido("pedidos.eventos", protegidos)).isTrue();
        assertThat(ReglasTopico.esProtegido("otro", protegidos)).isFalse();
    }
}
