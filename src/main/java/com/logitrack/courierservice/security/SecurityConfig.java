package com.logitrack.courierservice.security;

import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configura courier-service como Resource Server de JWT.
 *
 * <p>La firma RS256 se verifica localmente con la clave pública configurada.
 * No se consulta a security-service durante las peticiones. La matriz de
 * permisos deja el CRUD y la disponibilidad exclusivamente en manos de ADMIN y
 * reserva el endpoint interno de estado para ADMIN o para el REPARTIDOR dueño.
 * Todo endpoint no declarado queda denegado por defecto.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    /**
     * Construye un decodificador que valida firma, {@code exp}, emisor,
     * audiencia, {@code kid}, roles y sujeto sin llamadas remotas.
     *
     * @param properties configuración de confianza del servicio.
     * @return decodificador JWT local para RS256.
     * @throws IOException si no se puede leer la clave pública configurada.
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) throws IOException {
        try (var stream = properties.publicKeyLocation().getInputStream()) {
            var decoder = NimbusJwtDecoder.withPublicKey(RsaKeyConverters.x509().convert(stream))
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(properties.issuer()),
                    new JwtContractValidator(properties)));
            return decoder;
        }
    }

    /**
     * Define la cadena de filtros stateless y la matriz de acceso del
     * servicio.
     *
     * @param http configuración HTTP de Spring Security.
     * @param errors escritor de respuestas 401/403.
     * @param internalCourierAuthorization autorización por repartidor dueño.
     * @return cadena de filtros aplicada a todas las peticiones.
     * @throws Exception si Spring Security no puede construir la cadena.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityErrors errors,
                                            InternalCourierAuthorization internalCourierAuthorization)
            throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/internal/couriers/*/status")
                        .access(internalCourierAuthorization)
                        .requestMatchers("/api/v1/internal/couriers", "/api/v1/internal/couriers/**").denyAll()
                        .requestMatchers("/api/v1/couriers", "/api/v1/couriers/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, exception) ->
                                errors.write(request, response, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                errors.write(request, response, HttpStatus.FORBIDDEN)))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, exception) ->
                                errors.write(request, response, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                errors.write(request, response, HttpStatus.FORBIDDEN)))
                .build();
    }
}
