---
projeto: manager_accounting
registro: 012
titulo: Lançamentos do cliente
data: 2026-10-26
status: em revisão (branch feature/lancamentos, ainda não validado no Maven)
modulo: lancamentos
tags:
  - manager_accounting
  - registro
  - lancamentos
  - backend
  - frontend
---

# 012 - Lançamentos

Voltar para [[manager_accounting - Índice]] · Anterior: [[011 - Dashboard]]

## Resumo

Módulo de **lançamentos dos clientes**: compras, dívidas e despesas, com data, descrição, valor, categoria e comprovante opcional. O escritório lança, edita e exclui pelo backoffice. O app do cliente (por último no roadmap) vai gravar na mesma tabela com origem `CLIENTE`.

Antes deste registro, o PR #5 (`feature/dashboard`) foi mergeado na `main` em 26/10/2026.

- **Branch:** `feature/lancamentos`, criada a partir da `main`.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/6

## Decisões (26/10/2026)

| Tema | Decisão |
|---|---|
| Categorias | **Fixas**, iguais para todos os escritórios (19 categorias, "Outros" por último). O escritório não cadastra categorias. |
| Edição cruzada | O **contador pode alterar e excluir qualquer lançamento**, desde que seja de um cliente do escritório dele (inclusive os que o cliente fez pelo app). |
| Cliente x lançamentos do escritório | O cliente **não tem acesso** aos lançamentos feitos pelo escritório para alterar. Regra a aplicar quando a API do app existir. |
| Campos | Básico + comprovante: tipo, data, descrição, valor, categoria e um arquivo opcional. |
| Perfis | Administrador e usuário comum podem lançar, editar e excluir. |

## Regras

- **Isolamento:** toda consulta filtra pelo `tenant_id` do token. Lançamento ou cliente de outro escritório responde 404. O banco também garante: a FK `(cliente_id, tenant_id)` aponta para `cliente (id, tenant_id)`.
- **Cliente inativo:** não recebe lançamento novo. Os existentes continuam visíveis e editáveis.
- **Um lançamento não muda de cliente.** Se foi lançado no cliente errado, exclui e lança de novo.
- **Data:** não pode ser no futuro nem antes de 01/01/2000. "Hoje" é calculado no fuso do escritório (`app.lancamentos.fuso-horario`, padrão `America/Sao_Paulo`).
- **Valor:** maior que zero, até 999.999.999.999,99, no máximo duas casas.
- **Descrição:** até 200 caracteres, espaços repetidos viram um só. A busca não diferencia acentos ("cafe" acha "Café"), pela coluna `busca`.
- **Exclusão lógica:** `excluido_em` e `excluido_por_usuario_id`. Some das listas, dos totais e do dashboard, mas fica no banco com o histórico e o comprovante.
- **Edição simultânea:** editar, excluir e mexer no comprovante pedem a `versao` que a tela carregou. Versão diferente responde 409 `EDICAO_SIMULTANEA`.
- **Selo "Editado":** `editado_em` é preenchido quando os dados ou o comprovante mudam depois da criação. Salvar sem mudar nada não grava nem marca.
- **Histórico:** tabela `lancamento_historico`, uma linha por ação (`CRIAR`, `EDITAR`, `EXCLUIR`) com o estado logo depois dela em JSONB, quem fez e a origem. A tela compara uma linha com a anterior para mostrar o que mudou.

## Comprovante

- **Formatos:** PDF, JPG, PNG e WEBP, até **10 MB**.
- **Formato pelo conteúdo:** o tipo vem dos primeiros bytes do arquivo, não do nome nem do Content-Type enviado. Um HTML renomeado para `.jpg` é recusado.
- **Armazenamento:** em disco, atrás da interface `ComprovanteStorage`, em `<diretorio>/<tenantId>/<uuid>.<ext>`. A chave é gerada pelo sistema, e toda leitura confere que o caminho não sai da pasta base. Em produção, a ideia é trocar por S3 sem mexer no serviço.
- **Transação:** o arquivo novo é apagado se a transação for desfeita. O antigo (troca ou remoção) só é apagado depois do commit.
- **Download:** `GET /api/lancamentos/{id}/comprovante`, como anexo, com `nosniff` e `no-store`. A tela baixa com o token e mostra a imagem ou o PDF num diálogo.
- **Configuração:** `COMPROVANTES_DIRETORIO` (padrão `./data/comprovantes`, já no `.gitignore`) e `COMPROVANTES_TAMANHO_MAXIMO`.

## API

| Método | Rota | Corpo / parâmetros | Retorno |
|---|---|---|---|
| GET | `/api/lancamentos/categorias` | | Lista `{valor, rotulo}` |
| GET | `/api/lancamentos` | `clienteId`, `tipo`, `categoria`, `de`, `ate` (yyyy-MM-dd), `busca`, `pagina`, `tamanho` | Página + `totais` por tipo e `valorTotal` do filtro inteiro |
| GET | `/api/lancamentos/{id}` | | Lançamento |
| GET | `/api/lancamentos/{id}/historico` | | Ações, da mais antiga para a mais recente |
| POST | `/api/lancamentos` | `clienteId`, `tipo`, `data`, `descricao`, `valor`, `categoria` | 201 + lançamento |
| PUT | `/api/lancamentos/{id}` | mesmos campos + `versao` | Lançamento |
| DELETE | `/api/lancamentos/{id}?versao=` | | 204 |
| POST | `/api/lancamentos/{id}/comprovante?versao=` | multipart, campo `arquivo` | Lançamento |
| DELETE | `/api/lancamentos/{id}/comprovante?versao=` | | Lançamento |
| GET | `/api/lancamentos/{id}/comprovante` | | Arquivo |

O dashboard (`GET /api/dashboard`) ganhou `lancamentosMes` (totais do mês atual por tipo, com o total do mês anterior) e `ultimosLancamentos` (5 últimos).

## Telas

- **Menu Lançamentos** (`/app/lancamentos`): lançamentos de todos os clientes. No topo ficam os totais por tipo, e clicar num tipo filtra por ele. Os filtros são cliente, busca na descrição, categoria e período (tudo, este mês, mês passado ou datas livres), e ficam todos na URL.
- **Filtro por cliente:** busca por nome ou CPF/CNPJ (com ou sem pontuação), incluindo clientes inativos. Fica na URL como `?cliente=<id>`, e ao abrir um link com ele o nome é carregado da API. Com o filtro ativo, "Novo lançamento" já vem com esse cliente (dá para trocar).
- **Ficha do cliente:** seção "Lançamentos" abaixo do cadastro, com a mesma lista já filtrada pelo cliente.
- **Novo e editar lançamento:** diálogo com busca de cliente (só ativos), tipo, data, valor em reais, descrição, categoria e comprovante (anexar, trocar ou remover).
- **Menu de cada linha:** editar, ver comprovante, histórico e excluir (com confirmação).
- **Visão geral:** blocos "Lançamentos do mês" (total, comparação com o mês passado e barras por tipo, com atalho para a lista filtrada) e "Últimos lançamentos".
- **Selos:** "Pelo app" (origem cliente), "Editado" e "Comprovante".
- **Celular:** cada linha vira um cartão, com o menu de ações no canto.

## Arquivos

| Arquivo | O que faz |
|---|---|
| `db/migration/V9__lancamento.sql` | Tabelas `lancamento` e `lancamento_historico`, constraints e índices |
| `modules/lancamento/Lancamento.java` | Entidade, com as regras de editar, excluir e anexar |
| `modules/lancamento/LancamentoService.java` | Regras de negócio, validação, comprovante e histórico |
| `modules/lancamento/LancamentoController.java` | Rotas `/api/lancamentos` |
| `modules/lancamento/LancamentoTotais.java` | Totais por tipo com o mesmo filtro da lista (Criteria) |
| `modules/lancamento/CategoriaLancamento.java` | Lista fixa de categorias com rótulo |
| `modules/lancamento/comprovante/*` | Armazenamento em disco, detecção do formato e limpeza do nome |
| `modules/dashboard/*` | Totais do mês e últimos lançamentos |
| `core/exception/GlobalExceptionHandler.java` | Erros de upload (413), parâmetro ou arquivo ausente |
| `web/src/features/lancamentos/*` | Painel, diálogos, serviço, tipos e formatação |
| `web/src/pages/lancamentos/LancamentosPage.tsx` | Página do menu |
| `web/src/pages/inicio/InicioPage.tsx` | Blocos de lançamentos no dashboard |

## Como foi validado

- **Migration:** aplicada do V1 ao V9 num PostgreSQL 16. As constraints foram testadas uma a uma: cliente de outro escritório, valor zero, tipo e categoria fora da lista, comprovante incompleto e descrição só com espaços. As consultas do dashboard rodaram com dados de dois escritórios.
- **Backend:** compilado contra stubs das bibliotecas, porque o Maven Central é bloqueado no ambiente do Claude. Um harness rodou 24 cenários com o armazenamento em disco de verdade: isolamento, validações, conflito de versão, exclusão lógica, histórico, comprovante (formato pelo conteúdo, limite, troca, rollback e commit, caminho fora da pasta) e dashboard.
- **Front:** `npm run build` e `npm run lint` sem erros. Fluxos testados no Playwright com API simulada, em desktop, tablet e celular, sem rolagem horizontal: filtrar, criar com comprovante, ver comprovante, histórico, editar e excluir.

## Falta validar (Marcos)

```bash
cd backend
mvn test
mvn spring-boot:run
```

Depois, no navegador:

1. Lançar uma despesa com foto do recibo.
2. Abrir o comprovante.
3. Editar o valor e ver o histórico.
4. Excluir o lançamento.
5. Conferir os totais no Início.

## Próximos passos

- **API do app do cliente** (`/app`): pareamento pelo token e lançamentos do próprio cliente, sem alterar os feitos pelo escritório.
- **Relatórios e exportação** por período.
- **Comprovantes em S3** quando for para produção.
