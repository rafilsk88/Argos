# ADR-002: PostgreSQL como banco principal

**Status:** aceita

## Contexto
Dados de custo são relacionais e analíticos: filtros por organização, período, serviço e conta, com agregações frequentes. Também há dados semiestruturados (fatores de uma anomalia, plano JSON do assistente).

## Problema
Qual banco atende o fato de custo, as agregações e o JSON auditável?

## Alternativas
1. **PostgreSQL (escolhida):** SQL robusto, particionamento declarativo, views materializadas, `jsonb`.
2. **MongoDB:** flexível para JSON, mas agregações relacionais e integridade referencial ficam por conta da aplicação.
3. **Data warehouse (ClickHouse/BigQuery):** excelente para escala analítica, excessivo e caro para o MVP.

## Decisão
PostgreSQL 16 com Flyway. `cost_records` é particionada por mês de uso; `cost_daily_agg` é uma view materializada com índice único (permite `REFRESH ... CONCURRENTLY`); `jsonb` guarda fatores de anomalia, plano do assistente e metadados de auditoria. Índices compostos começam por `organization_id`.

## Consequências
- (+) Integridade (FKs, constraints), transações e consultas expressivas em um só sistema.
- (+) Partições mensais mantêm índices pequenos e permitem descartar meses antigos.
- (−) Partições futuras precisam existir antes da ingestão: a função `ensure_cost_partition` cria sob demanda, com limitação documentada para meses já presentes na partição default.
- (−) Limite de escala vertical; V2 prevê réplica de leitura, e um warehouse só seria avaliado com dados em ordem de bilhões de linhas.
