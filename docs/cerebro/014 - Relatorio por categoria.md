---
projeto: manager_accounting
registro: 014
titulo: Relatório de gastos por categoria
data: 2026-10-26
status: concluído (na main desde 26/10/2026; falta rodar mvn test localmente)
modulo: relatorios
tags:
  - manager_accounting
  - registro
  - relatorios
  - backend
  - frontend
---

# 014 - Relatório por categoria

Voltar para [[manager_accounting - Índice]] · Anterior: [[013 - Relatorios]]

## Resumo

Terceiro relatório do menu **Relatórios**: **Gastos por categoria**, em PDF e Excel, com a logo e a cor do escritório. Mostra quanto foi gasto em cada categoria no período, separado em compras, dívidas e despesas, e a fatia de cada uma no total.

Foi a opção 7 da lista de sugestões, escolhida em 26/10/2026.

- **Branch:** `feature/relatorio-categoria`.
- **Pull request:** ver o link no fim do registro.

## Regras

- **Cliente opcional:** sem cliente escolhido, o relatório soma todos os clientes do escritório e mostra quantos clientes lançaram em cada categoria. Com um cliente, só os lançamentos dele (cliente inativo é aceito; cliente de outro escritório responde 404).
- **Período:** as mesmas regras dos outros relatórios (obrigatório, até um ano).
- Só aparecem as **categorias com lançamento**, do maior total para o menor. No empate, vale a ordem da lista de categorias.
- **Percentual:** fatia do total do período, com uma casa decimal, calculada no backend.
- Lançamentos **excluídos** não entram.

## Arquivos gerados

- **PDF:** caixas com os totais por tipo e uma tabela com Categoria, Lançamentos, Clientes (só no relatório de todos), Compras, Dívidas, Despesas, Total e "% do total", com uma barrinha proporcional à fatia de cada categoria.
- **Excel:** os totais de cada linha e da coluna são fórmulas, e o percentual também (`total da linha / total geral`). Tem filtro e cabeçalho congelado.
- **Nome do arquivo:** `categorias-<cliente ou escritório>-<período>.pdf|xlsx`.

## API

| Método | Rota | Parâmetros | Retorno |
|---|---|---|---|
| GET | `/api/relatorios/categorias` | `de`, `ate`, `clienteId` (opcional) | Linhas por categoria, totais e quantos clientes lançaram |

## Arquivos

| Arquivo | O que faz |
|---|---|
| `modules/relatorio/RelatorioConsultas.java` | SQL por categoria e contagem de clientes |
| `modules/relatorio/RelatorioService.java` | Ordenação, percentual e totais |
| `modules/relatorio/dto/CategoriasResponse.java` | Resposta da API |
| `web/src/features/relatorios/gerarPdf.ts` e `gerarExcel.ts` | `pdfPorCategoria` e `excelPorCategoria` |
| `web/src/pages/relatorios/RelatoriosPage.tsx` | Cartão "Gastos por categoria" |

## Como foi validado

- **SQL:** testado no PostgreSQL 16 para todos os clientes e para um cliente. Ficam de fora os lançamentos excluídos, os fora do período e os de outro escritório.
- **Backend:** um harness rodou 10 cenários dos relatórios. Para este, os casos foram ordenação, percentual, empate, cliente vazio e outro escritório.
- **Arquivos:** baixados pelo navegador e conferidos. O PDF foi renderizado em imagem, e o LibreOffice recalculou as fórmulas do Excel e chegou ao mesmo total.
- **Front:** build e lint sem erros. A tela foi testada no desktop e no celular.

## Falta validar (Marcos)

```bash
cd backend
mvn test
```

Depois, no navegador, gere o relatório de todos os clientes e de um cliente.
