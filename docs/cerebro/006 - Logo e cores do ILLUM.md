---
projeto: manager_accounting
registro: 006
titulo: Logo e cores do escritório ILLUM
data: 2026-10-19
status: concluído (direto na main, a pedido)
modulo: tenants
tags:
  - manager_accounting
  - registro
  - tenant
  - tema
---

# 006 - Logo e cores do ILLUM

Voltar para [[manager_accounting - Índice]] · Anterior: [[005 - Clientes]]

## Resumo

O escritório ILLUM ganhou logo e cores no backoffice. Alteração feita **direto na `main`**, a pedido (exceção à regra de sempre usar branch).

## O que entrou

| Item | Valor |
|---|---|
| GUID do tenant | `20911ebb-5ac2-4e81-866d-b79e1de891e4` (fixo, para casar com a pasta da logo) |
| Logo | `web/public/tenants/20911ebb-5ac2-4e81-866d-b79e1de891e4/logo.jpg` (256 × 256, reduzida da imagem original) |
| Cor primária | `#730710` (vinho do fundo da logo) |
| Cor secundária | `#e1a34d` (dourado das letras) |
| SQL | `docs/sql/tenant-illum.sql`: cria o tenant "ILLUM Contabilidade" com o GUID fixo e grava o tema. Pode rodar mais de uma vez |

As cores foram medidas na própria logo (mediana dos pixels do fundo e das letras), não escolhidas de cabeça.

## Como fica na tela

- Menu lateral em vinho escurecido `#600913` (ajuste automático de contraste do registro 004).
- Botões e links em `#730710`, com contraste de sobra para o texto branco.
- Item ativo do menu com a régua dourada `#e1a34d` (contraste 6,2:1 com o fundo do menu).

## Como aplicar

```bash
docker compose exec -T postgres psql -U manager -d manager_accounting < docs/sql/tenant-illum.sql
```

Se o ILLUM já estava cadastrado com outro GUID, o arquivo explica como trocar o GUID do tenant existente. No fim do arquivo há o modelo para criar o usuário de login do ILLUM.

## Validação

- [x] SQL executado duas vezes em PostgreSQL 16 com as migrations e o seed: um tenant, tema gravado
- [x] Tela conferida no Chromium com a logo e as cores novas
