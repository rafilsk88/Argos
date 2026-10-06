-- V4: agregação diária para o dashboard (evita varrer a tabela de fatos a cada requisição)

CREATE MATERIALIZED VIEW cost_daily_agg AS
SELECT organization_id,
       usage_date,
       cloud_account_id,
       service,
       region,
       COALESCE(team_tag, 'untagged') AS team_tag,
       SUM(amount)                    AS amount,
       COUNT(*)                       AS record_count
FROM cost_records
GROUP BY organization_id, usage_date, cloud_account_id, service, region, COALESCE(team_tag, 'untagged')
WITH DATA;

-- O índice único habilita REFRESH MATERIALIZED VIEW CONCURRENTLY (sem bloquear leituras)
CREATE UNIQUE INDEX uq_cost_daily_agg
    ON cost_daily_agg (organization_id, usage_date, cloud_account_id, service, region, team_tag);
CREATE INDEX idx_cost_daily_agg_org_date_service ON cost_daily_agg (organization_id, usage_date, service);
