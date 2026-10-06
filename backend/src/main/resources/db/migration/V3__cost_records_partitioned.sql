-- V3: fato de custo diário, particionado por mês de uso (usage_date).
-- A PK inclui a chave de partição (exigência do PostgreSQL).

CREATE TABLE cost_records (
    id                uuid          NOT NULL DEFAULT gen_random_uuid(),
    organization_id   uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    cloud_account_id  uuid          NOT NULL REFERENCES cloud_accounts (id) ON DELETE CASCADE,
    ingestion_job_id  uuid          REFERENCES ingestion_jobs (id) ON DELETE SET NULL,
    usage_date        date          NOT NULL,
    service           varchar(120)  NOT NULL,
    region            varchar(40)   NOT NULL DEFAULT 'global',
    usage_type        varchar(200),
    resource_id       varchar(300),
    team_tag          varchar(80),
    amount            numeric(18,6) NOT NULL,
    currency          varchar(3)    NOT NULL,
    -- hash da linha normalizada: reprocessar o mesmo CSV não duplica custo
    row_hash          varchar(64)   NOT NULL,
    created_at        timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (id, usage_date),
    CONSTRAINT uq_cost_records_row UNIQUE (organization_id, row_hash, usage_date)
) PARTITION BY RANGE (usage_date);

-- Índices no pai são propagados para todas as partições
CREATE INDEX idx_cost_records_org_date_service ON cost_records (organization_id, usage_date, service);
CREATE INDEX idx_cost_records_org_date_account ON cost_records (organization_id, usage_date, cloud_account_id);
CREATE INDEX idx_cost_records_org_team_date    ON cost_records (organization_id, team_tag, usage_date);

-- Partição de segurança: dados fora do intervalo pré-criado não são rejeitados
CREATE TABLE cost_records_default PARTITION OF cost_records DEFAULT;

-- Cria a partição do mês de p_month (idempotente). A ingestão chama esta função antes de inserir.
-- Limitação conhecida: criar partição de um mês que já tem linhas na partição default falha;
-- nesse caso é preciso mover as linhas (documentado em docs/DATABASE.md).
CREATE OR REPLACE FUNCTION ensure_cost_partition(p_month date) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    start_date date := date_trunc('month', p_month)::date;
    end_date   date := (date_trunc('month', p_month) + interval '1 month')::date;
    part_name  text := format('cost_records_y%sm%s', to_char(start_date, 'YYYY'), to_char(start_date, 'MM'));
BEGIN
    IF to_regclass(part_name) IS NULL THEN
        EXECUTE format('CREATE TABLE %I PARTITION OF cost_records FOR VALUES FROM (%L) TO (%L)',
                       part_name, start_date, end_date);
    END IF;
END;
$$;

-- Pré-cria 36 meses a partir de jan/2025 (cobre os dados sintéticos de demonstração)
DO $$
DECLARE
    m date := DATE '2025-01-01';
BEGIN
    WHILE m < DATE '2028-01-01' LOOP
        PERFORM ensure_cost_partition(m);
        m := (m + interval '1 month')::date;
    END LOOP;
END;
$$;
