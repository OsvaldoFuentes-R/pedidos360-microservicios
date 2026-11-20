package cl.duoc.pedidos360.bff.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/** Todo endpoint exige un JWT válido de Azure CIAM; solo el health check queda público. */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SeguridadProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // usa el bean 'corsConfigurationSource' de abajo
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SeguridadProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.jwkSetUri()).build();

        // Siempre: firma (JWK) y vigencia (exp/nbf). Opcionales: issuer y audiencia.
        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>();
        if (props.issuer() != null && !props.issuer().isBlank()) {
            validadores.add(JwtValidators.createDefaultWithIssuer(props.issuer()));
        } else {
            validadores.add(JwtValidators.createDefault());
        }
        if (props.audience() != null && !props.audience().isBlank()) {
            validadores.add(new JwtAudienceValidator(props.audience()));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadores));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SeguridadProperties props) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(props.corsOrigenes());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}