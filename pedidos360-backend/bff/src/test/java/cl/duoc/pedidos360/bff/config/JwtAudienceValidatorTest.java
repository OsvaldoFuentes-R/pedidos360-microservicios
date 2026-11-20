package cl.duoc.pedidos360.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAudienceValidatorTest {

    private Jwt token(List<String> audiencia) {
        return Jwt.withTokenValue("t").header("alg", "none").claim("aud", audiencia)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    @Test
    void aceptaUnTokenConLaAudienciaEsperada() {
        JwtAudienceValidator validador = new JwtAudienceValidator("api://1234-abcd");

        assertThat(validador.validate(token(List.of("1234-ABCD"))).hasErrors()).isFalse();
        assertThat(validador.validate(token(List.of("otra", "api://1234-abcd/"))).hasErrors()).isFalse();
    }

    @Test
    void rechazaUnTokenDeOtraAudiencia() {
        JwtAudienceValidator validador = new JwtAudienceValidator("api://1234-abcd");

        assertThat(validador.validate(token(List.of("api://otra-api"))).hasErrors()).isTrue();
    }

    @Test
    void rechazaUnTokenSinAudiencia() {
        JwtAudienceValidator validador = new JwtAudienceValidator("1234-abcd");
        Jwt sinAudiencia = Jwt.withTokenValue("t").header("alg", "none").claim("sub", "x")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

        assertThat(validador.validate(sinAudiencia).hasErrors()).isTrue();
    }
}
