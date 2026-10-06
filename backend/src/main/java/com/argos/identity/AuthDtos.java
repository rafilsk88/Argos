package com.argos.identity;

import com.argos.shared.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** DTOs de entrada e saída da API de autenticação. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 160) String organizationName,
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            // BCrypt só considera os primeiros 72 bytes; o limite evita falsa sensação de segurança
            @NotBlank @Size(min = 10, max = 72, message = "A senha deve ter entre 10 e 72 caracteres") String password) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
    }

    public record CurrentUserResponse(UUID userId, UUID organizationId, String email, Role role) {
    }
}
