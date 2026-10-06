package com.argos.shared.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "argos.security")
public record ArgosSecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Login login) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET precisa ter ao menos 32 caracteres") String secret,
            @NotBlank String issuer,
            long accessTtlSeconds,
            long refreshTtlSeconds) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record Login(@Min(1) int maxFailedAttempts, @Min(1) long lockMinutes) {
    }
}
