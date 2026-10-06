# Banco de dados

Migrations versionadas com Flyway em `backend/src/main/resources/db/migration`. O Hibernate roda com `ddl-auto: validate`: o schema pertence às migrations.

| Migration | Conteúdo |
|---|---|
| V1 | `organizations`, `roles`, `users`, `refresh_tokens` |
| V2 | `cloud_accounts`, `ingestion_jobs` (idempotência por `content_hash`) |
| V3 | `cost_records` particionada por mês + função `ensure_cost_partition` |
| V4 | view materializada `cost_daily_agg` |
| V5 | `anomalies`, `recommendations` |
| V6 | `assistant_queries`, `audit_log`, `feature_flags` |

## Pontos de atenção

- **Idempotência:** `uq_ingestion_jobs_content` impede o mesmo arquivo duas vezes por organização; `uq_cost_records_row` (organização + `row_hash` + data) impede linhas duplicadas se o job for reprocessado.
- **Partições:** a PK de `cost_records` inclui `usage_date` (exigência do PostgreSQL). Meses de jan/2025 a dez/2027 são pré-criados; fora disso, a ingestão chama `ensure_cost_partition(data)`. Se já existirem linhas desse mês na partição `cost_records_default`, a criação falha e é preciso mover as linhas antes.
- **View materializada:** atualizar com `REFRESH MATERIALIZED VIEW CONCURRENTLY cost_daily_agg` após cada ingestão concluída.
- **Tipos:** colunas de texto usam `varchar` (não `char`) para a validação do Hibernate não divergir.
- **Estado de verificação:** as migrations foram escritas mas **ainda não executadas** contra um PostgreSQL real; o teste com Testcontainers entra na Etapa 9.
