package cl.duoc.pedidos360.bff.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Locale;

/** Rechaza los tokens cuya audiencia (claim "aud") no incluye la de esta API. Acepta con o sin prefijo "api://". */
public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audienciaEsperada;

    public JwtAudienceValidator(String audienciaEsperada) {
        this.audienciaEsperada = normalizar(audienciaEsperada);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience() != null
                && token.getAudience().stream().anyMatch(a -> normalizar(a).equals(audienciaEsperada))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "La audiencia del token no corresponde a esta API", null));
    }

    private static String normalizar(String audiencia) {
        String valor = audiencia == null ? "" : audiencia.trim();
        if (valor.regionMatches(true, 0, "api://", 0, 6)) {
            valor = valor.substring(6);
        }
        return valor.replaceAll("/$", "").toLowerCase(Locale.ROOT);
    }
}
