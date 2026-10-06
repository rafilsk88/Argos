# Arquitetura do Argos

O Argos é um **monólito modular** em Java/Spring Boot, acompanhado de **um serviço Python separado** (pipeline de dados, detecção de anomalias e ponte com a LLM). A separação do serviço Python tem motivo: linguagem, ecossistema (pandas/estatística) e ciclo de vida de deploy diferentes.

## Visão geral

```mermaid
flowchart LR
    U[Usuário] --> FE[Frontend React]
    FE --> RP[Reverse Proxy]
    RP --> BE[Backend Spring Boot]
    BE --> PG[(PostgreSQL)]
    BE --> RD[(Redis)]
    BE -- publica job --> MQ[[RabbitMQ]]
    MQ -- consome --> AI[AI/Data Service FastAPI]
    AI -- resultados --> BE
    AI --> LLM[Provedor LLM]
    BE -. métricas e traces .-> OBS[OTel / Prometheus / Grafana / Loki]
    AI -. métricas e traces .-> OBS
```

## Módulos do backend

| Módulo | Responsabilidade | Estado |
|---|---|---|
| `shared` | Erros padronizados, correlationId, propriedades de segurança, config | implementado |
| `identity` | Cadastro, login, JWT, refresh rotativo, RBAC, bloqueio por tentativas | implementado |
| `organizations` | Tenants (organizações) | implementado |
| `ingestion` | Upload de CUR (CSV), jobs assíncronos, status e retry | próxima etapa |
| `costs` | Consultas e agregações de custo | planejado |
| `anomalies` | Resultados e ciclo de vida das anomalias | planejado |
| `recommendations` | Recomendações por economia estimada | planejado |
| `assistant` | Perguntas em linguagem natural (plano JSON validado) | planejado |
| `audit` | Trilha de auditoria e endpoint administrativo | planejado |
| `exports` | Exportação CSV/JSON | planejado |

### Regras de modularidade

1. Cada módulo expõe uma **interface pública** (ex.: `OrganizationApi`); repositórios e entidades são internos.
2. Um módulo **nunca acessa as tabelas de outro**; referências entre módulos são por `UUID`, sem `@ManyToOne` entre módulos.
3. `organizationId` vem **sempre do JWT** (`AuthenticatedUser`), nunca do corpo ou da URL: isolamento de tenant em toda consulta.
4. Candidatos naturais a virar serviços: `ingestion` e `assistant` (carga e perfil de falha distintos).

## Fluxo do assistente (feature WOW)

```mermaid
sequenceDiagram
    participant U as Usuário
    participant B as Backend
    participant L as LLM
    participant D as PostgreSQL
    U->>B: Pergunta em português
    B->>L: Pergunta + catálogo de métricas permitidas
    L-->>B: Plano de consulta em JSON (nunca SQL)
    B->>B: Valida plano contra whitelist (métricas, dimensões, janelas)
    alt plano inválido
        B-->>U: Rejeitado (registrado em assistant_queries)
    else plano válido
        B->>D: Consulta parametrizada com escopo do tenant
        D-->>B: Resultado agregado
        B->>L: Apenas o resultado agregado
        L-->>B: Explicação em linguagem natural
        B-->>U: Resposta
    end
```

A LLM nunca vê o banco, nunca executa SQL e nunca recebe dados brutos de outro tenant.

## Decisões

- [ADR-001 Monólito modular](ADR/ADR-001-modular-monolith.md)
- [ADR-002 PostgreSQL](ADR/ADR-002-postgresql.md)
- [ADR-003 RabbitMQ em vez de Kafka](ADR/ADR-003-message-broker.md)
