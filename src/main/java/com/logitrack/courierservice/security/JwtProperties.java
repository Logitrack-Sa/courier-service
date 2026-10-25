package com.logitrack.courierservice.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades necesarias para validar localmente los JWT emitidos por
 * security-service.
 *
 * <p>La clave pública se monta por entorno y nunca se versiona en el
 * repositorio. El {@code kid}, el emisor y la audiencia son valores no
 * sensibles que forman parte del contrato de identidad del Sprint 2.</p>
 *
 * @param publicKeyLocation ubicación de la clave pública RSA en formato PEM.
 * @param keyId identificador de la clave que debe coincidir con el header
 *              {@code kid} del token.
 * @param issuer emisor esperado en el claim {@code iss}.
 * @param audience audiencia esperada dentro del claim {@code aud}.
 */
@Validated
@ConfigurationProperties("logitrack.security")
public record JwtProperties(
        @NotNull Resource publicKeyLocation,
        @NotBlank String keyId,
        @NotBlank String issuer,
        @NotBlank String audience) {
}
