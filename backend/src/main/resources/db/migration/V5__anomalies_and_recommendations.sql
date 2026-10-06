-- V5: anomalias (detectadas por estatística) e recomendações (regras explícitas)

CREATE TABLE anomalies (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    cloud_account_id     uuid          REFERENCES cloud_accounts (id) ON DELETE CASCADE,
    series_key           varchar(300)  NOT NULL,
    service              varchar(120)  NOT NULL,
    detected_on          date          NOT NULL,
    observed_amount      numeric(18,6) NOT NULL,
    expected_amount      numeric(18,6) NOT NULL,
    score                numeric(12,4) NOT NULL,
    severity             varchar(10)   NOT NULL,
    impact_amount        numeric(18,6) NOT NULL,
    currency             varchar(3)    NOT NULL,
    contributing_factors jsonb         NOT NULL DEFAULT '[]'::jsonb,
    explanation          text,
    explanation_source   varchar(10),
    status               varchar(15)   NOT NULL DEFAULT 'OPEN',
    created_at           timestamptz   NOT NULL DEFAULT now(),
    -- idempotência: reexecutar a detecção não duplica a mesma anomalia
    CONSTRAINT uq_anomalies_series_day UNIQUE (organization_id, series_key, detected_on),
    CONSTRAINT ck_anomalies_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_anomalies_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT ck_anomalies_source CHECK (explanation_source IS NULL OR explanation_source IN ('RULES', 'LLM', 'MOCK'))
);

CREATE INDEX idx_anomalies_org_date ON anomalies (organization_id, detected_on DESC);
CREATE INDEX idx_anomalies_org_open ON anomalies (organization_id, severity) WHERE status = 'OPEN';

CREATE TABLE recommendations (
    id                         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id            uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    cloud_account_id           uuid          REFERENCES cloud_accounts (id) ON DELETE CASCADE,
    type                       varchar(30)   NOT NULL,
    service                    varchar(120)  NOT NULL,
    resource_id                varchar(300),
    title                      varchar(200)  NOT NULL,
    description                text          NOT NULL,
    estimated_monthly_savings  numeric(18,6) NOT NULL,
    currency                   varchar(3)    NOT NULL,
    status                     varchar(15)   NOT NULL DEFAULT 'OPEN',
    created_at                 timestamptz   NOT NULL DEFAULT now(),
    updated_at                 timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_recommendations_type CHECK (type IN ('IDLE_RESOURCE', 'RIGHTSIZING', 'DATA_TRANSFER', 'UNATTACHED_STORAGE')),
    CONSTRAINT ck_recommendations_status CHECK (status IN ('OPEN', 'ACCEPTED', 'DISMISSED', 'IMPLEMENTED')),
    CONSTRAINT ck_recommendations_savings CHECK (estimated_monthly_savings >= 0)
);

CREATE INDEX idx_recommendations_org_savings ON recommendations (organization_id, estimated_monthly_savings DESC)
    WHERE status = 'OPEN';
