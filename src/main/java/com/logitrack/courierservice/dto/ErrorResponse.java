package com.logitrack.courierservice.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> details
) {
    public static ErrorResponse of(int status, String error, String code, String message, String path, List<FieldViolation> details) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path, details);
    }

    public record FieldViolation(
            String field,
            String message
    ) {}
}