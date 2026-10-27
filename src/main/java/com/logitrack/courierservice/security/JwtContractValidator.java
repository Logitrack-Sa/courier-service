package com.logitrack.courierservice.security;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida las reglas de contrato que no cubre el validador estándar de Spring
 * Security, sin consultar al emisor por cada petición.
 *
 * <p>Además de la firma RS256 y del emisor comprobados por el decodificador,
 * se exige un {@code sub} UUID, roles conocidos, un {@code kid} esperado, la
 * audiencia de courier-service y una ventana temporal coherente.</p>
 */
final class JwtContractValidator implements OAuth2TokenValidator<Jwt> {

    private static final Set<String> ROLES = Set.of("ADMIN", "CLIENTE", "REPARTIDOR");
    private static final long MAX_CLOCK_SKEW_SECONDS = 30;

    private final JwtProperties properties;

    /**
     * Crea el validador con la configuración de confianza del servicio.
     *
     * @param properties propiedades de clave, emisor, audiencia y {@code kid}.
     */
    JwtContractValidator(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * Comprueba la forma del token contra el contrato publicado.
     *
     * @param jwt token ya firmado y decodificado localmente.
     * @return resultado exitoso o un error {@code invalid_token}.
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
            if (hasValidRoles(jwt) && hasExpectedKeyId(jwt) && hasExpectedAudience(jwt)
                    && hasValidTemporalWindow(jwt)) {
                return OAuth2TokenValidatorResult.success();
            }
        } catch (RuntimeException ignored) {
            // Claims ausentes o con tipos incorrectos también invalidan el token.
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "JWT fuera del contrato de courier-service", null));
    }

    private boolean hasValidRoles(Jwt jwt) {
        Object claim = jwt.getClaims().get("roles");
        return claim instanceof List<?> roles && !roles.isEmpty()
                && roles.stream().allMatch(role -> role instanceof String value && ROLES.contains(value));
    }

    private boolean hasExpectedKeyId(Jwt jwt) {
        return properties.keyId().equals(jwt.getHeaders().get("kid"));
    }

    private boolean hasExpectedAudience(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        return audience != null && audience.contains(properties.audience());
    }

    private boolean hasValidTemporalWindow(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();
        return issuedAt != null
                && expiresAt != null
                && issuedAt.isBefore(expiresAt)
                && !issuedAt.isAfter(Instant.now().plusSeconds(MAX_CLOCK_SKEW_SECONDS));
    }
}
