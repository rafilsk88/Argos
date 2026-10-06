package com.argos.shared.security;

import java.util.UUID;

/**
 * Principal autenticado extraído do JWT. Todo módulo usa organizationId daqui para escopo de tenant;
 * nunca um organizationId vindo do corpo ou da URL da requisição.
 */
public record AuthenticatedUser(UUID userId, UUID organizationId, String email, Role role) {
}
