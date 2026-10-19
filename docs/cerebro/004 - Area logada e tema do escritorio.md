---
projeto: manager_accounting
registro: 004
titulo: Área logada e tema do escritório
data: 2026-10-19
status: em revisão (branch feature/area-logada-clientes, ainda não validado no Maven)
modulo: backoffice
commits:
  - Área logada do backoffice com o tema do escritório e módulo de clientes
tags:
  - manager_accounting
  - registro
  - backoffice
  - tema
  - frontend
---

# 004 - Área logada e tema do escritório

Voltar para [[manager_accounting - Índice]] · Anterior: [[003 - Recuperacao de senha]] · Próximo: [[005 - Clientes]]

## Resumo

Depois do login, o contador agora entra num backoffice de verdade: menu lateral na cor do escritório com a logo, cabeçalho com o nome do escritório e o menu do usuário (nome, e-mail, login e Sair), e a página atual no meio. Tenants continuam cadastrados direto no banco.

- **Branch:** `feature/area-logada-clientes`, criada a partir da `feature/recuperacao-senha` (que ainda não está na `main`). A `main` não foi alterada.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/area-logada-clientes. Faça o merge depois da `feature/login` e da `feature/recuperacao-senha`, ou abra o PR contra a `feature/recuperacao-senha`.
- Este registro e o [[005 - Clientes]] estão no mesmo commit.

## O que mudou na tela

| Parte | Como ficou |
|---|---|
| Menu lateral | Fundo na cor primária do escritório (escurecida se precisar, ver "Contraste"). Logo no topo; sem logo, monograma com as iniciais + nome. Itens: Início e Clientes. O item ativo tem uma régua na cor secundária |
| Cabeçalho | "Escritório" + nome do escritório à esquerda; à direita, avatar com as iniciais do contador e o primeiro nome. O menu abre com nome completo, e-mail, login e **Sair** |
| Início (`/app`) | Saudação com o primeiro nome e a faixa "Carteira de clientes" (ativos, com acesso ao app e % dos ativos, inativos). Sem clientes, mostra o convite para cadastrar |
| Celular e tablet (até 959 px) | O menu vira gaveta aberta pelo botão ☰. Fecha no X, no Esc, tocando fora ou escolhendo um item |
| Aba do navegador | "Nome do escritório · Backoffice" depois do login |
| Endereço desconhecido em `/app/...` | Página "não encontrada" dentro do layout |

## Contraste das cores do escritório

Cada escritório escolhe as próprias cores, então o app não confia que elas tenham contraste. A partir de `cor_primaria` e `cor_secundaria` são calculadas (em `web/src/theme/theme.ts`):

| Variável CSS | Regra |
|---|---|
| `--color-primary-strong` | A primária escurecida até o texto branco ter contraste 4,5:1 (WCAG AA). Usada em botões, links e foco |
| `--color-sidebar` | A primária escurecida até contraste 7:1 com o branco. Fundo do menu |
| `--color-sidebar-accent` | A secundária, se tiver contraste 3:1 com o fundo do menu; senão, branco. Régua do item ativo e monograma |

Exemplos conferidos: verde `#0f766e` (o do escritório de exemplo), vinho `#7a1018` com dourado `#d4a64a`, amarelo `#f5d90a` e até branco `#ffffff` continuam legíveis.

## Backend

| Arquivo | O que faz |
|---|---|
| `modules/usuario/UsuarioController` | `GET /api/usuario/me` → `{ login, nomeCompleto, email }` do contador logado (id vindo do token) |
| `modules/usuario/UsuarioLogadoResponse` | Resposta acima |

O tema continua vindo de `GET /api/tenant/tema` (registro 002).

## Front

| Arquivo | O que faz |
|---|---|
| `layouts/BackofficeLayout` + `backoffice.css` | Menu lateral, cabeçalho, menu do usuário, gaveta no celular, link "Pular para o conteúdo" |
| `theme/theme.ts` | Cálculo de contraste e das variáveis CSS do tema |
| `theme/ThemeProvider` | Aplica as variáveis e o título da aba |
| `features/usuario/usuarioService` | Busca o contador logado |
| `features/auth/AuthProvider` + `AuthContext` | Agora guardam também o `usuario`; tema e usuário são buscados em paralelo depois do login (se um falhar, o backoffice abre assim mesmo) |
| `pages/inicio/InicioPage` + `inicio.css` | Página inicial |
| `pages/nao-encontrado/NaoEncontradoPage` | 404 dentro do layout |
| `components/icons` | Ícones do backoffice (mesmo traço do login) |
| `components/ui/Dialog`, `ToastProvider`, `useToast` | Diálogo modal (`<dialog>` nativo) e avisos rápidos com `aria-live` |
| `hooks/useMediaQuery`, `utils/texto` | Detecção de tela pequena e iniciais |
| `index.css` | Tokens (cores neutras, espaçamentos, raios, fontes) e componentes base (botões, alertas, diálogo, aviso, skeleton) |
| `routes/router` | `/app` com rotas filhas; `HomePage` removida |
| `main.tsx` | `index.css` passa a carregar antes dos estilos dos módulos |

## Logo do escritório

Igual ao combinado no registro 002: o arquivo fica em `web/public/tenants/<guid>/<arquivo>` e o nome do arquivo em `tenant_tema.logo_arquivo`. Se o arquivo não existir, aparece o monograma (a tela não quebra). Logos quadradas com fundo próprio e logos transparentes funcionam; a altura máxima é 56 px.

## Acessibilidade

- Link "Pular para o conteúdo" é o primeiro Tab da página.
- Gaveta fechada no celular fica `inert` (fora da tela e fora do Tab). Ao abrir, o foco vai para o X; ao fechar, volta para o ☰.
- Foco visível em todos os controles; menus fecham com Esc.
- Animações respeitam "reduzir movimento".

## Validação

- [x] Front: `npm run build` (TypeScript estrito) e `oxlint` sem erros nem avisos
- [x] Telas no Chromium com a API simulada, em 1440, 1280, 820, 390 e 360 px, sem rolagem horizontal
- [x] Tema verde (exemplo) e vinho/dourado com logo quadrada
- [x] Teclado: primeiro Tab, gaveta com foco e Esc
- [x] Tela de login conferida de novo (sem mudança visual)
- [ ] **Backend compilar e passar nos testes** (`mvn test`): o Maven Central é bloqueado no ambiente de criação. O código novo foi compilado com `javac` contra assinaturas das bibliotecas usadas, mas falta o build real. Rodar na sua máquina

## Pontos de atenção

- A fonte Figtree vem do Google Fonts (já era assim no login); sem internet, cai na fonte do sistema.
- Ainda não existe tela para o escritório trocar logo e cores: continua pelo banco.
