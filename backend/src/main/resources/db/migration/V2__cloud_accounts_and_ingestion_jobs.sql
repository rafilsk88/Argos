-- V2: contas AWS da organização e jobs de ingestão (processamento assíncrono)

CREATE TABLE cloud_accounts (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    provider         varchar(10)  NOT NULL DEFAULT 'AWS',
    external_id      varchar(32)  NOT NULL,
    display_name     varchar(160) NOT NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_cloud_accounts_external UNIQUE (organization_id, provider, external_id),
    CONSTRAINT ck_cloud_accounts_provider CHECK (provider IN ('AWS'))
);

CREATE TABLE ingestion_jobs (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    uploaded_by      uuid         REFERENCES users (id) ON DELETE SET NULL,
    file_name        varchar(255) NOT NULL,
    file_size_bytes  bigint       NOT NULL,
    storage_path     varchar(500) NOT NULL,
    -- SHA-256 do conteúdo: o mesmo arquivo enviado duas vezes não gera dois jobs (idempotência)
    content_hash     varchar(64)  NOT NULL,
    status           varchar(20)  NOT NULL DEFAULT 'PENDING',
    attempts         integer      NOT NULL DEFAULT 0,
    rows_total       integer      NOT NULL DEFAULT 0,
    rows_valid       integer      NOT NULL DEFAULT 0,
    rows_rejected    integer      NOT NULL DEFAULT 0,
    error_message    text,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    started_at       timestamptz,
    finished_at      timestamptz,
    CONSTRAINT uq_ingestion_jobs_content UNIQUE (organization_id, content_hash),
    CONSTRAINT ck_ingestion_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'DEAD_LETTERED')),
    CONSTRAINT ck_ingestion_jobs_counts CHECK (rows_total >= 0 AND rows_valid >= 0 AND rows_rejected >= 0 AND attempts >= 0)
);

CREATE INDEX idx_ingestion_jobs_org_created ON ingestion_jobs (organization_id, created_at DESC);
CREATE INDEX idx_ingestion_jobs_open ON ingestion_jobs (status) WHERE status IN ('PENDING', 'PROCESSING');
