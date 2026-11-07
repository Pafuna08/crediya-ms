package co.com.crediya.api.config;

import co.com.crediya.api.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Reglas de acceso del microservicio de solicitudes.
 * Los tokens los emite el microservicio de autenticación; aquí solo se valida su firma
 * con la misma clave secreta compartida.
 * - POST /api/v1/solicitud solo para el rol CLIENTE.
 * - Todo lo demás exige un token válido.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    public static final String CLAIM_DOCUMENTO = "documento";
    static final String CLAIM_ROL = "rol";
    static final String EMISOR = "crediya-autenticacion";
    static final String ROL_CLIENTE = "CLIENTE";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        http
                // API sin estado: cada petición trae su token, no hay sesión ni formularios
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitud").hasRole(ROL_CLIENTE)
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/prometheus", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, ex) -> {
                            log.warn("Acceso sin token válido a {}: {}", request.getRequestURI(), ex.getMessage());
                            escribirError(jsonMapper, request, response, HttpStatus.UNAUTHORIZED,
                                    "Debe iniciar sesión con un token válido para acceder a este recurso");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            log.warn("Acceso denegado a {} para {}", request.getRequestURI(), usuario(request));
                            escribirError(jsonMapper, request, response, HttpStatus.FORBIDDEN,
                                    "No tiene permisos para realizar esta acción");
                        }));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secreto) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Además de la firma, verifica que no esté vencido y que lo haya emitido CrediYa
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(EMISOR)));
        return decoder;
    }

    /** Convierte el claim "rol" del token (ej. CLIENTE) en la autoridad ROLE_CLIENTE que usa hasRole. */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private static String usuario(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? "anónimo" : request.getUserPrincipal().getName();
    }

    private static void escribirError(JsonMapper jsonMapper, HttpServletRequest request, HttpServletResponse response,
                                      HttpStatus status, String mensaje) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), List.of(mensaje), request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
