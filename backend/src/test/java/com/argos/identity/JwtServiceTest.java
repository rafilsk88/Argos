package com.argos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.argos.shared.security.ArgosSecurityProperties;
import com.argos.shared.security.AuthenticatedUser;
import com.argos.shared.security.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-32-characters-0123456789";

    private static JwtService serviceWith(String secret, long accessTtlSeconds) {
        return new JwtService(new ArgosSecurityProperties(
                new ArgosSecurityProperties.Jwt(secret, "argos", accessTtlSeconds, 604800),
                new ArgosSecurityProperties.Cors(List.of("http://localhost:5173")),
                new ArgosSecurityProperties.Login(5, 15)));
    }

    private static AuthenticatedUser sampleUser() {
        return new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "ana@empresa.com.br", Role.ANALYST);
    }

    @Test
    void roundTripPreservesIdentityAndRole() {
        JwtService service = serviceWith(SECRET, 900);
        AuthenticatedUser user = sampleUser();

        Optional<AuthenticatedUser> parsed = service.parse(service.generateAccessToken(user));

        assertThat(parsed).contains(user);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = serviceWith("another-secret-with-at-least-32-characters-abcdef", 900)
                .generateAccessToken(sampleUser());

        assertThat(serviceWith(SECRET, 900).parse(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = serviceWith(SECRET, -10);

        assertThat(service.parse(service.generateAccessToken(sampleUser()))).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(serviceWith(SECRET, 900).parse("nao.e.um-jwt")).isEmpty();
    }
}
