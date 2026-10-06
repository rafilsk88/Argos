-- V6: assistente (plano JSON auditável), trilha de auditoria e feature flags por organização

CREATE TABLE assistant_queries (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id    uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id            uuid          REFERENCES users (id) ON DELETE SET NULL,
    question           text          NOT NULL,
    query_plan         jsonb,
    validation_status  varchar(10)   NOT NULL,
    rejection_reason   varchar(300),
    answer             text,
    prompt_version     varchar(20)   NOT NULL,
    tokens_input       integer       NOT NULL DEFAULT 0,
    tokens_output      integer       NOT NULL DEFAULT 0,
    estimated_cost     numeric(12,6) NOT NULL DEFAULT 0,
    latency_ms         integer,
    created_at         timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_assistant_validation CHECK (validation_status IN ('VALID', 'REJECTED', 'ERROR')),
    CONSTRAINT ck_assistant_tokens CHECK (tokens_input >= 0 AND tokens_output >= 0)
);

CREATE INDEX idx_assistant_queries_org_created ON assistant_queries (organization_id, created_at DESC);

CREATE TABLE audit_log (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    actor_user_id   uuid         REFERENCES users (id) ON DELETE SET NULL,
    action          varchar(60)  NOT NULL,
    target_type     varchar(60),
    target_id       varchar(80),
    metadata        jsonb        NOT NULL DEFAULT '{}'::jsonb,
    ip_address      varchar(45),
    correlation_id  varchar(64),
    created_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_org_created ON audit_log (organization_id, created_at DESC);
CREATE INDEX idx_audit_log_org_action  ON audit_log (organization_id, action, created_at DESC);

CREATE TABLE feature_flags (
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    flag_key        varchar(60)  NOT NULL,
    enabled         boolean      NOT NULL DEFAULT false,
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (organization_id, flag_key)
);
