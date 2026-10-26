---
projeto: manager_accounting
registro: 011
titulo: Dashboard (página Início)
data: 2026-10-22
status: em revisão (branch feature/dashboard, ainda não validado no Maven)
modulo: dashboard
tags:
  - manager_accounting
  - registro
  - dashboard
  - backend
  - frontend
---

# 011 - Dashboard

Voltar para [[manager_accounting - Índice]] · Anterior: [[010 - Ajustes nos clientes]]

## Resumo

A página **Início** (Visão geral) virou um dashboard do escritório, montado só com os dados que já existem: clientes, acesso ao app e equipe. Os números de **lançamentos** (compras, dívidas e despesas) entram aqui quando esse módulo for feito.

Antes deste registro, as branches dos registros 002 a 010 foram mergeadas na `main` (22/10/2026).

- **Branch:** `feature/dashboard`, criada a partir da `main` já com tudo mergeado.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/dashboard

## O que a tela mostra

| Bloco | Conteúdo | Quem vê |
|---|---|---|
| Carteira de clientes | Clientes ativos, novos este mês (comparado com o mês anterior), com acesso ao app (% dos ativos) e inativos | Todos |
| Novos clientes por mês | Barras dos últimos 6 meses, contando o atual (que fica na cor do escritório) | Todos |
| Composição da carteira | Pessoa jurídica x física (ativos) e PJ por regime tributário, com "Não informado" quando houver | Todos |
| Sem acesso ao app | Total de ativos sem token e os 5 mais recentes, com atalho para a ficha (onde se gera o token) | Todos |
| Cadastrados recentemente | Os 5 últimos cadastros, com selo de inativo ou app ativo | Todos |
| Equipe do escritório | Usuários ativos e convites aguardando aceite, com atalho para Usuários | Só administrador |

Sem nenhum cliente cadastrado, a tela mostra só o convite para cadastrar (e a equipe, para o administrador). Se a API falhar, aparece o aviso com "Tentar novamente".

## Regras

- **Mês do cadastro no fuso do escritório:** o banco guarda as datas em UTC; um cliente cadastrado às 23h de 31/10 em Fortaleza (02h de 01/11 em UTC) conta como outubro. O fuso fica em `app.dashboard.fuso-horario` (padrão `America/Sao_Paulo`, mesma hora de Fortaleza).
- **Novos por mês** conta todos os cadastros do mês, mesmo de clientes desativados depois. A **composição** conta só os ativos.
- **Usuários ativos** são os que já concluíram o primeiro acesso; quem ainda não aceitou o convite aparece em **convites aguardando aceite** (convite válido, não aceito e não cancelado).
- O bloco da equipe nem é consultado quando quem pede não é administrador (`equipe: null` na resposta).

## API

| Método | Rota | Perfil | Retorno |
|---|---|---|---|
| GET | `/api/dashboard` | Contador (qualquer perfil) | `carteira`, `novosPorMes`, `composicao`, `semAcessoApp`, `recentes`, `equipe` |

O endpoint antigo `GET /api/clientes/resumo` continua existindo, mas a tela não usa mais.

## Arquivos

| Arquivo | O que faz |
|---|---|
| `modules/dashboard/DashboardController` | Rota `GET /api/dashboard`; tenant do token e perfil pela autoridade `ROLE_ADMIN` |
| `modules/dashboard/DashboardService` | Monta a resposta: série de 6 meses com zeros, mês atual x anterior, composição com todos os regimes |
| `modules/dashboard/DashboardConsultas` | Consultas SQL de agregação (`NamedParameterJdbcTemplate`), todas filtradas por `tenant_id` |
| `modules/dashboard/DashboardProperties` | Fuso horário (`app.dashboard.fuso-horario`) |
| `modules/dashboard/dto/DashboardResponse` | Formato da resposta |
| `application.yml` | `app.dashboard.fuso-horario` (variável `DASHBOARD_FUSO_HORARIO`) |
| `features/dashboard/*` (web) | Tipos, chamada da API e rótulos de mês |
| `pages/inicio/InicioPage.tsx` + `inicio.css` | Tela nova, responsiva (desktop, tablet e celular) |

Sem migration nova: o dashboard só lê tabelas que já existem.

## Validação feita

- **Consultas no PostgreSQL 16** com as migrations V1 a V8 e dados de dois escritórios: totais, série por mês (incluindo o caso de virada de mês em UTC), composição, listas e equipe batem com o esperado, e nenhum dado do outro escritório aparece. As consultas testadas são exatamente as strings do código (capturadas do Java compilado).
- **Lógica do serviço:** os casos do `DashboardServiceTest` rodados à parte (série com zeros, fuso, virada de ano, equipe só para admin, sem consulta extra quando todos têm app).
- **Front:** `npm run build` e `npm run lint` sem erros; telas conferidas com dados de exemplo no tema da ILLUM e no tema neutro, em 1440, 820 e 390 px, mais os estados vazio e de erro, sem rolagem horizontal.
- **Não validado:** `mvn test` e o back-end rodando de verdade, porque o Maven não roda no ambiente do Claude.

## Como testar

1. `mvn test` (inclui o `DashboardServiceTest` novo) e suba o back-end.
2. Entre como administrador: a Visão geral mostra todos os blocos, inclusive a equipe.
3. Entre com um usuário comum: o bloco da equipe não aparece.
4. Cadastre um cliente: ele aparece em "Novos este mês", na barra do mês atual, em "Cadastrados recentemente" e em "Sem acesso ao app". Gere o token na ficha e volte: ele sai da lista e "Com acesso ao app" sobe.
5. Desative um cliente: ele sai dos ativos e da composição, mas continua contando no mês em que foi cadastrado.

## Pendências

- Números de lançamentos no dashboard (totais do mês por tipo, clientes com mais movimento, últimos lançamentos) junto com o módulo de lançamentos.
