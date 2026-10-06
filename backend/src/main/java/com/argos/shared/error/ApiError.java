package com.argos.shared.error;

import com.argos.shared.web.TraceContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Formato único de erro da API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldViolation> details) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message, String path, List<FieldViolation> details) {
        return new ApiError(Instant.now(), code.status().value(), code.name(), message, path,
                TraceContext.currentId(), details);
    }
}
