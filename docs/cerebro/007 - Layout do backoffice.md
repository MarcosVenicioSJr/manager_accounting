---
projeto: manager_accounting
registro: 007
titulo: Layout do backoffice (padrão LS Contábil)
data: 2026-10-19
status: em revisão (branch feature/layout-backoffice)
modulo: backoffice
tags:
  - manager_accounting
  - registro
  - backoffice
  - layout
  - frontend
---

# 007 - Layout do backoffice

Voltar para [[manager_accounting - Índice]] · Anterior: [[006 - Logo e cores do ILLUM]]

## Resumo

O menu e o cabeçalho do backoffice passaram a seguir a estrutura da tela de referência "LS Contábil - Clientes" (Claude Design). Foi copiado **só o layout**: cores, fontes e ícones continuam os do projeto, e as cores e a logo continuam vindo do tenant (banco).

- **Branch:** `feature/layout-backoffice`, criada a partir da `main`.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/layout-backoffice

## O que mudou

| Parte | Antes | Agora (como na referência) |
|---|---|---|
| Menu lateral | 248 px, fixo | 236 px, **recolhível para 76 px** (só ícones) pelo botão « » no rodapé. A escolha fica salva no navegador |
| Topo do menu | Logo ou monograma + nome | Logo/monograma redondo de 40 px + nome do escritório + "GESTÃO DE CLIENTES" |
| Itens do menu | Início, Clientes | **Visão geral**, Clientes (recolhido: ícone com dica e rótulo acessível) |
| Rodapé do menu | "Backoffice contábil" | Avatar + nome e e-mail do contador, **Sair** e o botão de recolher |
| Cabeçalho (72 px) | Escritório + menu do usuário | **Título e subtítulo da página**, "‹ Voltar para clientes" quando há, e as **ações da página à direita** (Novo cliente, Desativar cliente) |
| Página inicial | Saudação como título | Título "Visão geral" no cabeçalho; saudação e data por extenso no conteúdo |
| Celular e tablet | Gaveta | Continua gaveta (não recolhe). Ações da página descem para baixo do título, em largura total |

O menu do usuário no cabeçalho saiu: nome, e-mail e Sair agora ficam no rodapé do menu lateral, como na referência.

## Como as páginas preenchem o cabeçalho

Novo componente `layouts/PageHeader`:

```tsx
<PageHeader
  titulo="Clientes"
  subtitulo="20 clientes encontrados"
  voltar={{ para: '/app/clientes', rotulo: 'Voltar para clientes' }}
  acoes={<Link to="/app/clientes/novo" className="ui-btn ui-btn--primary">Novo cliente</Link>}
/>
```

Ele desenha o conteúdo dentro do cabeçalho do layout (portal do React). A página continua dona do estado dos botões, então "Desativar cliente" segue abrindo o diálogo da própria página.

## Arquivos

| Arquivo | O que mudou |
|---|---|
| `layouts/BackofficeLayout` | Nova estrutura do menu (marca, navegação, conta, recolher) e cabeçalho com o espaço da página |
| `layouts/PageHeader` + `headerSlot` | Título, subtítulo, voltar e ações no cabeçalho |
| `layouts/backoffice.css` | Novo layout, estado recolhido e responsivo |
| `pages/inicio`, `pages/clientes/*`, `pages/nao-encontrado` | Usam `PageHeader`; os títulos antigos dentro do conteúdo saíram |
| `features/clientes/clientes.css` | Limpeza de estilos que não são mais usados |

## O que não veio da referência (de propósito)

- Fontes Newsreader/Manrope/IBM Plex Mono e a paleta: ficam as do projeto e as do tenant.
- "Excluir" e "Editar cliente" no cabeçalho do detalhe: aqui o detalhe já é o formulário de edição, e não existe exclusão (só desativar).
- "3 usuários" no rodapé: o backoffice ainda não lista usuários do escritório; no lugar fica o e-mail do contador.

## Validação

- [x] `npm run build` e `oxlint` sem erros nem avisos
- [x] Telas no Chromium (API simulada): visão geral, lista, detalhe e cadastro, menu expandido e recolhido (e recolhido mantido após recarregar), tema ILLUM e tema de exemplo, celular 390 px com gaveta, sem rolagem horizontal
