package com.argos.identity;

import com.argos.identity.AuthDtos.LoginRequest;
import com.argos.identity.AuthDtos.RefreshRequest;
import com.argos.identity.AuthDtos.RegisterRequest;
import com.argos.identity.AuthDtos.TokenResponse;
import com.argos.organizations.OrganizationApi;
import com.argos.shared.error.BusinessException;
import com.argos.shared.error.ErrorCode;
import com.argos.shared.security.ArgosSecurityProperties;
import com.argos.shared.security.AuthenticatedUser;
import com.argos.shared.security.Role;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final OrganizationApi organizations;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ArgosSecurityProperties properties;
    private final Clock clock;
    /** Hash descartável: usado para gastar o mesmo tempo de CPU quando o e-mail não existe (anti-enumeração por timing). */
    private final String dummyHash;

    AuthService(UserRepository users, RefreshTokenRepository refreshTokens, OrganizationApi organizations,
                PasswordEncoder passwordEncoder, JwtService jwtService, ArgosSecurityProperties properties,
                Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("argos-dummy-password");
    }

    /** Cria a organização (tenant) e o primeiro usuário, que nasce ADMIN. */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, "E-mail já cadastrado");
        }
        UUID organizationId = organizations.createOrganization(request.organizationName());
        User user = users.save(new User(organizationId, email, passwordEncoder.encode(request.password()),
                request.fullName().trim(), Role.ADMIN));
        return issueTokens(user);
    }

    /**
     * noRollbackFor: a falha de login lança exceção, mas o contador de tentativas precisa ser gravado.
     * A mensagem é a mesma para e-mail inexistente, senha errada e conta desabilitada.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(LoginRequest request) {
        String email = normalize(request.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        Instant now = clock.instant();
        if (!user.isEnabled()) {
            throw invalidCredentials();
        }
        if (user.isLocked(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "Conta temporariamente bloqueada por tentativas inválidas. Tente novamente mais tarde.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            ArgosSecurityProperties.Login login = properties.login();
            user.registerFailedLogin(now, login.maxFailedAttempts(), Duration.ofMinutes(login.lockMinutes()));
            throw invalidCredentials();
        }
        user.resetLoginFailures();
        return issueTokens(user);
    }

    /**
     * Refresh rotativo: cada token só vale uma vez. Reapresentar um token já usado indica roubo ou replay,
     * então todas as sessões do usuário são revogadas.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse refresh(RefreshRequest request) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokens.findByTokenHash(hash(request.refreshToken()))
                .orElseThrow(AuthService::invalidRefreshToken);
        if (stored.isRevoked()) {
            refreshTokens.revokeAllForUser(stored.getUserId(), now);
            throw invalidRefreshToken();
        }
        if (stored.isExpired(now)) {
            throw invalidRefreshToken();
        }
        User user = users.findById(stored.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(AuthService::invalidRefreshToken);
        stored.revoke(now);
        return issueTokens(user);
    }

    /** Idempotente: token desconhecido ou já revogado não gera erro. */
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokens.findByTokenHash(hash(request.refreshToken()))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private TokenResponse issueTokens(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getOrganizationId(), user.getEmail(), user.getRole());
        String accessToken = jwtService.generateAccessToken(principal);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plusSeconds(properties.jwt().refreshTtlSeconds());
        refreshTokens.save(new RefreshToken(user.getId(), hash(rawRefresh), expiresAt));

        return new TokenResponse(accessToken, rawRefresh, "Bearer", jwtService.accessTtlSeconds());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "E-mail ou senha inválidos");
    }

    private static BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Sessão expirada. Faça login novamente.");
    }
}
