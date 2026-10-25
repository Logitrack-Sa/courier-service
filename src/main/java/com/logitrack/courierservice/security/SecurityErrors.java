package com.logitrack.courierservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.courierservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Escribe respuestas JSON homogéneas para fallos de autenticación y
 * autorización producidos en la cadena de filtros de Spring Security.
 *
 * <p>Se usa desde el entry point y el access denied handler para que un 401 o
 * un 403 mantengan el mismo formato que el resto de errores del servicio.</p>
 */
@Component
@RequiredArgsConstructor
public class SecurityErrors {

    private final ObjectMapper mapper;

    /**
     * Construye y escribe la respuesta de seguridad.
     *
     * @param request petición HTTP que originó el rechazo.
     * @param response respuesta HTTP donde se serializa el error.
     * @param status estado 401 o 403.
     * @throws IOException si no se puede escribir el cuerpo JSON.
     */
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                status == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : "ACCESS_DENIED",
                status == HttpStatus.UNAUTHORIZED
                        ? "Se requiere un JWT válido"
                        : "No tenés permiso para este recurso",
                request.getRequestURI(),
                List.of()));
    }
}
