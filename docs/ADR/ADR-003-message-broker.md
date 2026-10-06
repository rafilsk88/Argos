# ADR-003: RabbitMQ em vez de Kafka

**Status:** aceita

## Contexto
O upload de um relatório de custo deve responder rápido e processar em segundo plano (validação, normalização, agregação, detecção de anomalias), com retry e tratamento de falha.

## Problema
Qual broker atende jobs de ingestão com retry e dead-letter?

## Alternativas
1. **RabbitMQ (escolhida):** filas de trabalho, ack/nack, TTL, DLQ nativa, operação simples.
2. **Kafka:** log distribuído de alto throughput com replay; exige mais operação (partições, offsets, consumer groups).
3. **Sem broker (`@Async` ou job table):** simples, mas perde durabilidade e desacoplamento do serviço Python.

## Decisão
RabbitMQ. O padrão é de fila de tarefas, não de stream de eventos: não há necessidade de replay nem de milhões de mensagens por segundo. Retry com backoff via filas de espera, DLQ para falhas definitivas e idempotência por `content_hash` do arquivo (já modelada em `ingestion_jobs`).

## Consequências
- (+) Menor complexidade operacional e DLQ pronta.
- (+) Propagação de trace OpenTelemetry pelos headers da mensagem.
- (−) Sem replay histórico; se surgir necessidade de reprocessar eventos em larga escala, reavaliar Kafka (V3).
