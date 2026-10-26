package com.logitrack.courierservice.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * Autoriza el endpoint interno mínimo de estado de un repartidor.
 *
 * <p>ADMIN puede consultar cualquier identificador. REPARTIDOR solo puede
 * consultar el {@code courierId} firmado en su propio JWT. Cualquier otro rol,
 * un claim ausente o un identificador de ruta distinto se deniega antes de
 * invocar el controller.</p>
 */
@Component
public class InternalCourierAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String PATH_PREFIX = "/api/v1/internal/couriers/";
    private static final String PATH_SUFFIX = "/status";

    /**
     * Decide si la petición puede acceder al recurso interno solicitado.
     *
     * @param authenticationSupplier proveedor de la autenticación resuelta por el
     *                       Resource Server.
     * @param context contexto de la petición que incluye la URI.
     * @return decisión otorgada solo para ADMIN o para el REPARTIDOR dueño.
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                       RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        if (hasRole(authentication, "ADMIN")) {
            return new AuthorizationDecision(true);
        }
        if (!hasRole(authentication, "REPARTIDOR")) {
            return new AuthorizationDecision(false);
        }

        UUID requestedCourierId = requestedCourierId(context.getRequest());
        UUID signedCourierId = signedCourierId(jwtAuthentication.getToken());
        return new AuthorizationDecision(requestedCourierId != null && requestedCourierId.equals(signedCourierId));
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private static UUID requestedCourierId(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(PATH_PREFIX) || !path.endsWith(PATH_SUFFIX)) {
            return null;
        }
        String rawId = path.substring(PATH_PREFIX.length(), path.length() - PATH_SUFFIX.length());
        if (rawId.isEmpty() || rawId.contains("/")) {
            return null;
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static UUID signedCourierId(Jwt jwt) {
        try {
            String claim = jwt.getClaimAsString("courierId");
            return claim == null ? null : UUID.fromString(claim);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
