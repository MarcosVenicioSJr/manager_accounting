---
projeto: manager_accounting
registro: 008
titulo: Meu perfil (dados, troca de senha e de e-mail com confirmação por e-mail)
data: 2026-10-21
status: em revisão (branch feature/meu-perfil, ainda não validado no Maven)
modulo: perfil
tags:
  - manager_accounting
  - registro
  - perfil
  - seguranca
  - email
  - backend
  - frontend
---

# 008 - Meu perfil

Voltar para [[manager_accounting - Índice]] · Anterior: [[007 - Layout do backoffice]]

## Resumo

O contador tem a página **Meu perfil** (`/app/perfil`), aberta pelo nome dele no rodapé do menu lateral.

- **Nome e telefone** mudam na hora.
- **Senha:** a troca só acontece **depois que ele confirma pelo link enviado ao e-mail**. O pedido exige a senha atual; até a confirmação, a senha atual continua valendo.
- **E-mail:** mesmo esquema. O link vai para o **novo** endereço (prova que ele tem acesso), e o endereço antigo recebe um aviso depois da troca.

- **Branch:** `feature/meu-perfil`, criada a partir da `main`.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/meu-perfil

## Fluxo da troca de senha

1. No Meu perfil: senha atual + nova senha + confirmação → "Enviar link de confirmação".
2. A API confere a senha atual e a política, guarda o **hash BCrypt da nova senha** como pendente e envia "Confirme a troca de senha" para o e-mail atual. Resposta 202; nada mudou ainda.
3. A tela mostra "Esperando a confirmação da troca de senha", com a validade e o botão "Cancelar pedido".
4. O e-mail traz `<WEB_BASE_URL>/confirmar-alteracao#token=...`. A tela confere o link e pergunta: **"Confirmar troca de senha"**. Abrir o link não muda nada; só o clique confirma (leitores de e-mail que abrem links sozinhos não confirmam por engano).
5. Ao confirmar: a nova senha passa a valer, o bloqueio de login é zerado, os links de "Esqueceu sua senha?" pendentes são anulados, **todas as sessões são encerradas** e chega o e-mail "Sua senha foi alterada". A tela leva ao login.

## Fluxo da troca de e-mail

1. No Meu perfil: novo e-mail + senha atual → "Enviar link de confirmação".
2. O link "Confirme seu novo e-mail" vai para o **novo** endereço. O e-mail atual continua valendo.
3. Ao confirmar: o novo e-mail passa a valer, os links de recuperação enviados ao antigo são anulados e o **antigo recebe "Seu e-mail foi alterado"** com o novo parcialmente escondido (`ma***@empresa.com.br`). As sessões continuam abertas.

## Regras

| Tema | Decisão |
|---|---|
| Senha atual errada | Conta como tentativa errada de login: **5 erros bloqueiam por 15 minutos** (login e Meu perfil). A contagem é gravada mesmo com o erro (`noRollbackFor`) |
| Nova senha | Mesma política do sistema e **diferente da atual** |
| Link | 32 bytes aleatórios, depois do `#`; no banco só o SHA-256. Vale **30 minutos**, uso único, travado na confirmação (dois cliques não confirmam duas vezes) |
| Pedido novo | Anula o anterior do mesmo tipo: só o link mais recente vale |
| Limite | Por usuário e por tipo: 1 minuto entre pedidos e no máximo 5 por hora |
| Senha mudou por outro caminho | Se a senha mudar (ex.: "Esqueceu sua senha?") depois do pedido, o link da troca deixa de valer. O pedido guarda o SHA-256 do hash da senha da época e confere na confirmação |
| Cancelar | "Cancelar pedido" anula o link; nada muda |
| Confirmar sem estar logado | Funciona: o link pode ser aberto no celular. O token prova o acesso ao e-mail e o pedido já tinha exigido a senha atual |

> [!note] Escolhas minhas, não definidas por você
> A troca de e-mail com o mesmo esquema de confirmação (você pediu para a senha; para o e-mail fiz igual porque o e-mail é por onde se recupera a senha), o aviso ao e-mail antigo, encerrar todas as sessões depois da troca de senha, a senha atual contar para o bloqueio e o limite de 1 minuto / 5 por hora. Tudo configurável ou fácil de mudar.

## Banco (Flyway)

| Migration | Tabela |
|---|---|
| `V6__alteracao_pendente.sql` | `alteracao_pendente` (usuario_id, tipo `SENHA`/`EMAIL`, valor_novo, referencia, token_hash único, criado_em, expira_em, confirmado_em, invalidado_em). Índices em (usuario_id, tipo, criado_em) e expira_em. Limpeza diária às 03:30 |

## Endpoints

| Método e rota | Acesso | Corpo | Resposta |
|---|---|---|---|
| `GET /api/perfil` | Contador | | `{ login, nomeCompleto, email, telefone, nomeEscritorio, trocaSenhaPendente, trocaEmailPendente }` |
| `PUT /api/perfil` | Contador | `{ nomeCompleto, telefone }` | Perfil atualizado |
| `POST /api/perfil/senha` | Contador | `{ senhaAtual, novaSenha }` | 202 `{ enviadoPara, expiraEm }`; 400 senha atual errada, política, igual à atual ou aguarde |
| `POST /api/perfil/email` | Contador | `{ senhaAtual, novoEmail }` | 202 `{ enviadoPara, novoEmail, expiraEm }` |
| `DELETE /api/perfil/alteracoes/{SENHA\|EMAIL}` | Contador | | 204 |
| `POST /api/auth/confirmar-alteracao/validar` | Público | `{ token }` | `{ tipo, novoEmail }`; 400 link inválido |
| `POST /api/auth/confirmar-alteracao` | Público | `{ token }` | `{ tipo }`; 400 link inválido |

## Backend

| Arquivo | O que faz |
|---|---|
| `modules/perfil/PerfilService` | Dados, pedidos, cancelamento, validação e confirmação; limpeza diária |
| `modules/perfil/PerfilController`, `ConfirmacaoAlteracaoController` | Endpoints acima |
| `modules/perfil/AlteracaoPendente`, `AlteracaoPendenteRepository`, `TipoAlteracao` | Pedido pendente e consultas (com trava na confirmação) |
| `modules/perfil/PerfilEmails` | Os 4 e-mails: confirmar senha, senha alterada, confirmar novo e-mail, e-mail alterado (ao antigo) |
| `modules/perfil/PerfilProperties` | Validade e limites (`app.perfil.confirmacao.*`) |
| `modules/perfil/SenhaAtualIncorretaException` | 400 que não desfaz a contagem de tentativas |
| `core/mail/EmailLayout` | Moldura dos e-mails, extraída da recuperação de senha (agora compartilhada; os e-mails da recuperação saem idênticos, conferido byte a byte) |
| `modules/usuario/Usuario` | Novos `atualizarDados` e `alterarEmail` |
| `modules/auth/EmailPendente`, `AuthService` | Evento de e-mail e constantes de bloqueio ficaram públicos para o perfil reutilizar |
| `SecurityConfig` | Rotas públicas de confirmação |

Testes novos: `PerfilServiceTest` (16 casos).

## Front

| Arquivo | O que faz |
|---|---|
| `pages/perfil/PerfilPage` + `perfil.css` | Seções Dados pessoais, E-mail e Senha; formulários que abrem sob demanda, regras da senha, "Mostrar senhas", aviso de pendência com "Cancelar pedido" |
| `pages/confirmar-alteracao/ConfirmarAlteracaoPage` | Tela do link do e-mail (visual das telas de acesso): pergunta, confirmado, link inválido. Tira o token da barra de endereço |
| `features/perfil/perfilService` | Chamadas à API |
| `features/auth/AuthProvider` + `AuthContext` | `recarregarUsuario` (o nome no menu atualiza depois de salvar) |
| `layouts/BackofficeLayout` + `backoffice.css` | Nome do contador no rodapé do menu virou link para o Meu perfil |
| `routes/router` | `/app/perfil` e `/confirmar-alteracao` |

## Configuração

| Variável | Padrão |
|---|---|
| `PERFIL_CONFIRMACAO_TOKEN_TTL` | `30m` |
| `PERFIL_CONFIRMACAO_INTERVALO_MINIMO` | `1m` |
| `PERFIL_CONFIRMACAO_MAX_ENVIOS_POR_HORA` | `5` |

## Como testar

1. `docker compose up -d`, `mvn spring-boot:run` (o Flyway cria a tabela nova) e `npm run dev`.
2. Entre, clique no seu nome no rodapé do menu → Meu perfil.
3. **Senha:** "Alterar senha" → senha atual + nova → enviar. Confira que a senha antiga ainda funciona. Abra o Mailpit (http://localhost:8025), clique no link, confirme. A sessão cai; entre com a nova.
4. **E-mail:** "Alterar e-mail" → novo e-mail + senha atual. O link chega no novo endereço (no Mailpit todos chegam). Confirme; o antigo recebe "Seu e-mail foi alterado".
5. Abra o mesmo link de novo: "Link inválido".
6. Peça uma troca de senha, use "Esqueceu sua senha?" para trocar por outro caminho e depois abra o link do perfil: "Link inválido".
7. Erre a senha atual 5 vezes: bloqueia por 15 minutos.

## Validação

- [x] Migration `V6` em PostgreSQL 16 com as anteriores, o seed e o ILLUM; check do tipo e token único testados
- [x] Código Java novo compilado com `javac` contra as assinaturas das bibliotecas
- [x] **Fluxo do `PerfilService` executado de verdade** com repositórios em memória: 32 verificações (pedido não troca, link de uso único, pedido novo anula o anterior, cancelar, expiração, senha trocada por outro caminho, sessões encerradas, aviso ao e-mail antigo mascarado, bloqueio na 5ª senha errada)
- [x] E-mails da recuperação de senha idênticos aos de antes da refatoração (comparados byte a byte)
- [x] Front: `npm run build` e `oxlint` sem erros nem avisos
- [x] Telas no Chromium com API simulada: perfil, erros da senha, senha atual errada, pendências, confirmar e-mail, confirmar senha, link inválido; celular 390 px sem rolagem horizontal
- [ ] **`mvn test`** e backend real (Maven bloqueado no ambiente de criação)
- [ ] Roteiro "Como testar" com backend real e Mailpit
