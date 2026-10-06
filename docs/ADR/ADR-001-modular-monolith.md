# ADR-001: Monólito modular no backend

**Status:** aceita

## Contexto
O Argos tem um domínio coeso (identidade, ingestão, custos, anomalias, assistente) e um time pequeno. Precisa de entrega rápida, transações simples entre módulos e operação barata, sem fechar a porta para separação futura.

## Problema
Qual estilo de arquitetura equilibra simplicidade operacional e evolução?

## Alternativas
1. **Microsserviços desde o início:** isolamento forte, mas custo alto de deploy, observabilidade distribuída, consistência eventual e contratos entre serviços, sem carga que justifique.
2. **Monólito em camadas sem fronteiras:** simples, mas o acoplamento cresce e inviabiliza a separação depois.
3. **Monólito modular (escolhida):** um deploy, fronteiras explícitas por módulo.

## Decisão
Monólito modular. Cada módulo expõe uma interface pública, não acessa tabelas de outros módulos e referencia outros por `UUID`. O serviço Python é separado desde já porque a linguagem e o ciclo de vida são diferentes.

## Consequências
- (+) Um artefato, transações locais, depuração e testes mais simples.
- (+) Extrair `ingestion` ou `assistant` depois exige trocar chamadas em memória por HTTP/mensageria, sem reescrever o domínio.
- (−) As fronteiras dependem de disciplina; mitigação planejada: teste de arquitetura (ArchUnit) na etapa de testes.
- (−) Escala é por réplica do monólito inteiro até a extração.

**Quando migrar:** quando um módulo tiver perfil de carga ou cadência de release claramente distinto (ex.: ingestão em volume alto).
