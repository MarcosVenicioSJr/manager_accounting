---
projeto: manager_accounting
registro: 013
titulo: Relatórios (extrato do cliente e resumo do escritório)
data: 2026-10-26
status: em revisão (branch feature/relatorios, ainda não validado no Maven)
modulo: relatorios
tags:
  - manager_accounting
  - registro
  - relatorios
  - backend
  - frontend
---

# 013 - Relatórios

Voltar para [[manager_accounting - Índice]] · Anterior: [[012 - Lancamentos]]

## Resumo

Menu **Relatórios** com dois relatórios, cada um em **PDF** e **Excel**, com a logo e a cor do escritório:

- **Extrato do cliente:** todos os lançamentos de um cliente no período, com totais por tipo e por categoria.
- **Resumo do escritório:** uma linha por cliente com compras, dívidas, despesas e total no período.

- **Branch:** `feature/relatorios`, criada a partir da `main` depois do merge do registro 012.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/7

## Decisões (26/10/2026)

| Tema | Decisão |
|---|---|
| Formatos | PDF e Excel |
| Relatórios da primeira versão | Extrato do cliente e Resumo do escritório (o relatório por categoria ficou para depois) |
| Identidade | Logo e cor do escritório (tenant) no PDF e no Excel |
| Ordem do roadmap | Foco no backoffice; a administração da plataforma fica por último, depois do app do cliente |

## Como funciona

- O **backend entrega os dados já somados** (`/api/relatorios/...`), e o **navegador monta o arquivo**: PDF com jsPDF e jspdf-autotable, Excel com ExcelJS.
- Com isso o backend não ganha dependência nova. As bibliotecas ficam em arquivos separados do front e só carregam quando alguém clica em "Baixar".
- A cor usada nos arquivos é a mesma dos botões do backoffice (`--color-primary-strong`, com contraste garantido). A logo vem de `web/public/tenants/<guid>/`; sem logo, o PDF sai só com o nome do escritório.

## Regras

- **Período obrigatório**, de no máximo **um ano** (366 dias). Atalhos na tela: este mês, mês passado, este ano ou outro período.
- **Extrato:** aceita cliente inativo, para tirar o histórico de quem saiu do escritório. Cliente de outro escritório responde 404. O limite é de 5.000 lançamentos; acima disso, a mensagem pede um período menor. A ordem é do mais antigo para o mais recente, como num extrato.
- **Resumo:** entram os clientes ativos (com zero se não tiveram lançamento, em cinza no PDF) e os inativos que tiveram lançamento no período. A ordem é do maior total para o menor, depois por nome.
- **Lançamentos excluídos** não entram em nenhum relatório.
- **Excel:**
  - Datas e valores vão como data e número de verdade, no formato R$, e não como texto.
  - Os totais são fórmulas `SUM`, que continuam certas se a pessoa mexer nas linhas.
  - O cabeçalho fica congelado, com filtro.
  - O extrato tem duas abas: "Lançamentos" e "Totais".
- **PDF:**
  - A4 retrato, com cabeçalho na primeira página: logo, escritório, título, período e "Gerado em".
  - Caixas de totais por tipo.
  - O cabeçalho da tabela se repete em cada página, e nenhuma linha fica partida entre duas páginas.
  - Rodapé com "Página X de Y".
- **Nome do arquivo:** `extrato-<cliente>-2026-10.pdf` para um mês inteiro, ou `..._2026-01-01_a_2026-10-26.xlsx` para outros períodos.

## API

| Método | Rota | Parâmetros | Retorno |
|---|---|---|---|
| GET | `/api/relatorios/extrato` | `clienteId`, `de`, `ate` (yyyy-MM-dd) | Escritório, cliente, itens, totais por tipo e por categoria |
| GET | `/api/relatorios/resumo` | `de`, `ate` | Linhas por cliente, totais, clientes com e sem movimento |

## Telas

- **Relatórios** (`/app/relatorios`), no menu entre Lançamentos e Usuários: dois cartões, um para cada relatório. Cada cartão tem a escolha de período e os botões "Baixar PDF" e "Baixar Excel".
- **Ficha do cliente:** botão **"Exportar extrato"** na seção Lançamentos. Ele abre os Relatórios com o cliente já escolhido (`?cliente=<id>`).
- **Ajuste junto:** a lupa do campo de busca de cliente ficava em cima do texto (no formulário de lançamento e aqui). Corrigido.

## Arquivos

| Arquivo | O que faz |
|---|---|
| `modules/relatorio/RelatorioConsultas.java` | SQL do extrato e do resumo |
| `modules/relatorio/RelatorioService.java` | Validação do período, limite e totais |
| `modules/relatorio/RelatorioController.java` | Rotas `/api/relatorios` |
| `web/src/features/relatorios/gerarPdf.ts` | Monta os PDFs |
| `web/src/features/relatorios/gerarExcel.ts` | Monta as planilhas |
| `web/src/features/relatorios/arquivo.ts` | Nome do arquivo, download, logo e datas |
| `web/src/features/relatorios/periodo.ts` e `PeriodoSeletor.tsx` | Atalhos e validação do período |
| `web/src/pages/relatorios/RelatoriosPage.tsx` | Tela |

## Como foi validado

- **SQL:** rodado num PostgreSQL 16 com dois escritórios. O teste conferiu que ficam de fora os lançamentos excluídos, os fora do período, os de outro escritório e o cliente inativo sem lançamento; e que o último dia do mês entra.
- **Backend:** compilado contra stubs das bibliotecas, porque o Maven Central é bloqueado no ambiente do Claude. Um harness rodou 7 cenários: totais, extrato vazio, isolamento, limite de 5.000, período e ordenação do resumo.
- **Arquivos:** baixados pelo navegador no Playwright e conferidos:
  - O PDF foi renderizado em imagem.
  - O texto do PDF foi extraído.
  - O Excel foi lido célula a célula, com formatos, filtro e congelamento.
  - O LibreOffice recalculou as fórmulas e chegou ao mesmo total.
  - Um extrato de 93 lançamentos gerou 6 páginas sem linha partida.
- **Front:** build e lint sem erros. A tela foi testada no desktop e no celular, sem rolagem horizontal.

## Falta validar (Marcos)

```bash
cd backend
mvn test
mvn spring-boot:run
cd ../web
npm install
```

O `npm install` instala as bibliotecas novas de PDF e Excel. Depois, gere um extrato de um cliente com lançamentos e abra o PDF e o Excel.
