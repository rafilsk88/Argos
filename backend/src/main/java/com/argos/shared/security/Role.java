package com.argos.shared.security;

/** Papéis de RBAC, verificados por @PreAuthorize nos controllers. */
public enum Role {
    ADMIN,
    ANALYST,
    VIEWER
}
