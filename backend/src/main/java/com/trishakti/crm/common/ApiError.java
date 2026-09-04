package com.trishakti.crm.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    public record FieldValidationError(String field, Object rejectedValue, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldValidationError> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }

    public Map<String, Object> asMap() {
        return Map.of("timestamp", timestamp, "status", status, "error", error,
                "message", message, "path", path, "fieldErrors", fieldErrors);
    }
}
