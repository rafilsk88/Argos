-- V1: tenants, papéis, usuários e refresh tokens
-- Obs.: colunas textuais usam varchar (não char) para o Hibernate validar o schema sem divergência de tipo.

CREATE TABLE organizations (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name                     varchar(160) NOT NULL,
    currency                 varchar(3)   NOT NULL DEFAULT 'BRL',
    monthly_ai_token_budget  integer      NOT NULL DEFAULT 200000,
    created_at               timestamptz  NOT NULL DEFAULT now(),
    updated_at               timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_organizations_budget CHECK (monthly_ai_token_budget >= 0)
);

CREATE TABLE roles (
    name        varchar(20)  PRIMARY KEY,
    description varchar(200) NOT NULL
);

INSERT INTO roles (name, description) VALUES
    ('ADMIN',   'Administra a organização, usuários, auditoria e feature flags'),
    ('ANALYST', 'Faz upload, consulta custos, usa o assistente e trata anomalias'),
    ('VIEWER',  'Somente leitura de dashboards e relatórios');

CREATE TABLE users (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id       uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    email                 varchar(254) NOT NULL,
    password_hash         varchar(100) NOT NULL,
    full_name             varchar(160) NOT NULL,
    role                  varchar(20)  NOT NULL REFERENCES roles (name),
    enabled               boolean      NOT NULL DEFAULT true,
    failed_login_attempts integer      NOT NULL DEFAULT 0,
    locked_until          timestamptz,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_at            timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

CREATE INDEX idx_users_organization ON users (organization_id);

CREATE TABLE refresh_tokens (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  varchar(64)  NOT NULL,
    expires_at  timestamptz  NOT NULL,
    revoked_at  timestamptz,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
