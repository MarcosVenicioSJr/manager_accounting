---
projeto: manager_accounting
registro: 003
titulo: Recuperação de senha por e-mail
data: 2026-10-19
status: em revisão (branch feature/recuperacao-senha, ainda não validado no Maven)
modulo: autenticacao
commits:
  - Recuperação de senha por e-mail (link de uso único, Mailpit, telas)
tags:
  - manager_accounting
  - registro
  - autenticacao
  - recuperacao-de-senha
  - email
  - backend
  - frontend
---

# 003 - Recuperação de senha por e-mail

Voltar para [[manager_accounting - Índice]] · Anterior: [[002 - Modulo de login]]

## Resumo

"Esqueceu sua senha?" funcionando de ponta a ponta. A pessoa informa o usuário e recebe no e-mail um link de uso único que vale 30 minutos. Pelo link ela cria uma nova senha. Depois disso todas as sessões abertas são encerradas e ela recebe um e-mail avisando da troca.

- **Branch:** `feature/recuperacao-senha`, criada a partir da `feature/login` (o login ainda não está na `main`). A `main` não foi alterada.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/recuperacao-senha. Faça o merge depois da `feature/login`, ou abra o PR contra a `feature/login`.

## Fluxo

1. Na tela de login, "Esqueceu sua senha?" leva a `/recuperar-senha`, onde a pessoa informa o **usuário** (o mesmo login da tela de entrada).
2. A API responde sempre a mesma mensagem, exista ou não o usuário. Se ele puder recuperar, o backend grava o token e envia o e-mail em segundo plano.
3. O e-mail traz o link `<WEB_BASE_URL>/redefinir-senha#token=...`.
4. A tela `/redefinir-senha` lê o token, apaga-o da barra de endereço e confere o link na API. Se o link valer, mostra o formulário de nova senha. Se não valer, mostra "Link inválido" com o botão "Pedir um novo link".
5. Ao salvar, a senha é trocada, o link é consumido, as sessões são encerradas e chega o e-mail "Sua senha foi alterada".

## Decisões

| Tema | Decisão |
|---|---|
| Como a pessoa se identifica | Pelo **usuário (login)**, como já estava no `docs/arquitetura.md`. O e-mail não é único entre escritórios, e quem não fez o primeiro acesso não tem e-mail cadastrado |
| Quem recebe o link | Usuário ativo, com tenant ativo e **com o primeiro acesso concluído** (é no primeiro acesso que o e-mail é cadastrado). Quem ainda não concluiu continua usando a senha temporária |
| Resposta do pedido | Sempre a mesma (HTTP 202), exista ou não o usuário, para ninguém descobrir logins pela recuperação |
| Envio do e-mail | Só depois do commit e em segundo plano (`@Async` + `@TransactionalEventListener`). A resposta não espera o SMTP, então o tempo de resposta não denuncia se o usuário existe |
| Token | 32 bytes aleatórios (base64url). No banco fica só o hash SHA-256 |
| Validade e uso | **30 minutos**, uso único. Um pedido novo anula os links anteriores, então só o mais recente vale |
| Token na URL | Fica depois do `#` (`/redefinir-senha#token=...`). Essa parte não vai para o servidor do front nem para outros sites no Referer, e a tela a apaga da barra de endereço ao abrir. Para a API o token vai no corpo, nunca na URL |
| Limite de envios | Por usuário: no mínimo **2 minutos** entre e-mails e no máximo **5 por hora**. Acima disso a resposta é a mesma, mas nada é enviado |
| Depois de redefinir | Zera as tentativas erradas e tira o bloqueio de login, **encerra todas as sessões** (refresh tokens revogados) e envia um e-mail de confirmação da troca |
| Uso simultâneo | O link é lido com trava de linha (`PESSIMISTIC_WRITE`), então dois envios ao mesmo tempo não usam o mesmo link duas vezes |
| Política da nova senha | A mesma do primeiro acesso: 8+ caracteres, letras, números e símbolos, até 72 bytes |
| Visual | As mesmas telas do login (painel azul-marinho à esquerda). O e-mail também é genérico ("Backoffice contábil") e cita o nome do escritório |

> [!note] Escolhas minhas, não definidas por você
> Validade de 30 minutos, limite de 2 minutos e 5 por hora, e-mail de confirmação, encerrar todas as sessões e não exigir que a nova senha seja diferente da atual. Tudo isso é configurável ou fácil de mudar; é só pedir.

## Banco (Flyway)

| Migration | Tabela |
|---|---|
| `V4__token_redefinicao_senha.sql` | `token_redefinicao_senha` (usuario_id, token_hash único, criado_em, expira_em, utilizado_em, invalidado_em). Índices em (usuario_id, criado_em) e expira_em |

Uma limpeza diária às 03:15 apaga os links expirados há mais de um dia.

## Endpoints (públicos)

| Método e rota | Corpo | Resposta |
|---|---|---|
| `POST /api/auth/recuperar-senha` | `{ login }` | 202 `{ mensagem }`, sempre igual |
| `POST /api/auth/redefinir-senha/validar` | `{ token }` | 204 se o link vale; 400 "Este link de redefinição é inválido ou expirou…" |
| `POST /api/auth/redefinir-senha` | `{ token, novaSenha }` | 204; 400 com link inválido ou senha fora da política |

## Backend

| Arquivo | O que faz |
|---|---|
| `modules/auth/RecuperacaoSenhaService` | Pedido do link (com limites), validação e redefinição; limpeza diária |
| `modules/auth/RecuperacaoSenhaController` | Os três endpoints acima |
| `modules/auth/TokenRedefinicaoSenha` + `TokenRedefinicaoSenhaRepository` | Entidade e consultas (busca com trava, anular pendentes, contagem para o limite) |
| `modules/auth/RecuperacaoSenhaEmails` | Textos dos e-mails (link e confirmação), em texto e HTML, com os nomes escapados |
| `modules/auth/EmailPendente` + `RecuperacaoSenhaEmailListener` | Evento e envio assíncrono depois do commit |
| `modules/auth/RecuperacaoSenhaProperties` | Validade do link e limites de envio |
| `core/mail/EmailService` + `EmailMessage` + `AppMailProperties` | Envio por SMTP (remetente configurável). Falha de envio vai para o log, sem destinatário nem link |
| `core/config/AppWebProperties` | Endereço do front para montar os links |
| `modules/usuario/Usuario` | Novos `redefinirSenha` (também zera o bloqueio) e `podeRecuperarSenhaPorEmail` |
| `modules/auth/RefreshTokenService` | Novo `revogarTodos` (encerra as sessões do usuário) |
| `SecurityConfig` / `ManagerAccountingApplication` | Rotas públicas novas e `@EnableAsync` |

Testes novos: `RecuperacaoSenhaServiceTest` (12 casos: hash no banco, link com `#token`, usuário inexistente, sem primeiro acesso, inativo, limites, link expirado/usado/inexistente, redefinição completa, senha fora da política) e `RecuperacaoSenhaEmailsTest` (3 casos).

## Front

| Arquivo | O que faz |
|---|---|
| `pages/recuperar-senha/RecuperarSenhaPage` | Campo usuário, validação, envio e estado "Verifique seu e-mail" com "Enviar novamente" |
| `pages/redefinir-senha/RedefinirSenhaPage` | Lê o token do `#`, limpa a URL, valida o link e mostra o formulário, "Link inválido" ou "Senha alterada" |
| `features/auth/NovaSenhaFields` | Campos de nova senha e confirmação com a lista de regras; agora usado também no primeiro acesso |
| `features/auth/authService` | `solicitarRecuperacaoSenha`, `validarLinkRedefinicao`, `redefinirSenha` |
| `features/auth/icons` + `auth.css` | Ícones (e-mail, ok, voltar) e estilos dos estados |
| `routes/router` | Rota `/redefinir-senha` |

## Configuração

| Variável | Padrão | Observação |
|---|---|---|
| `WEB_BASE_URL` | `http://localhost:5173` | Endereço do front usado nos links do e-mail. Em produção, o domínio real |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | Mailpit em desenvolvimento |
| `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | vazio / `false` | Para um SMTP real em produção |
| `MAIL_FROM` / `MAIL_FROM_NAME` | `no-reply@manageraccounting.local` / `Backoffice contábil` | Remetente |
| `RECUPERACAO_SENHA_TOKEN_TTL` | `30m` | Validade do link |
| `RECUPERACAO_SENHA_INTERVALO_MINIMO` | `2m` | Por usuário |
| `RECUPERACAO_SENHA_MAX_ENVIOS_POR_HORA` | `5` | Por usuário |

O SMTP tem tempo limite (5 s para conectar, 10 s para ler e escrever), para um servidor fora do ar não travar a fila de envio.

## Como testar

1. `docker compose up -d` (Postgres + Mailpit) e `mvn spring-boot:run` no backend. O Flyway cria a tabela nova.
2. Entre com `contador.exemplo` / `Temp@1234` e conclua o primeiro acesso, informando um e-mail qualquer. Depois clique em Sair.
3. Clique em "Esqueceu sua senha?", informe `contador.exemplo` e abra o Mailpit em http://localhost:8025.
4. Clique em "Redefinir senha" no e-mail, crie a nova senha e confira o segundo e-mail ("Sua senha foi alterada").
5. Abra o mesmo link de novo: deve aparecer "Link inválido".

## Validação

- [x] Migration `V4` executada em PostgreSQL 16 de teste, junto com as anteriores e o script de cadastro
- [x] Java: sem erros de sintaxe (checado com `javac`)
- [x] Modelos de e-mail executados de verdade (formatação e escape de HTML) e conferidos no navegador
- [x] Front: `npm run build` (TypeScript estrito) e `oxlint` sem erros
- [x] Telas conferidas no Chromium com a API simulada: pedido do link, campo vazio, "Verifique seu e-mail", nova senha, "Senha alterada", link inválido, página sem token; 390 e 360 px sem rolagem horizontal; token removido da URL
- [ ] **Backend compilar e passar nos testes** (`mvn test`): não foi possível no ambiente de criação (o Maven Central é bloqueado lá). Rodar na sua máquina
- [ ] Fluxo completo com backend real e Mailpit (roteiro acima)

## Pontos de atenção

- Se o backend não subir, o mais provável é detalhe de mapeamento JPA ou de API de biblioteca, já que o código não foi compilado no ambiente de criação. Mande o log.
- O caminho de quem tem o link gravado faz um pouco mais de trabalho no banco que o de quem não tem (alguns milissegundos), mas o envio do e-mail, que é a parte lenta, não entra no tempo de resposta.
- Ainda não há **limite por IP** (mesmo motivo do login: depende de como o proxy repassa o IP). O limite é por usuário.
- Em produção, `WEB_BASE_URL` precisa apontar para o domínio real; senão o link do e-mail vai para `localhost`.
- Se o SMTP falhar, a pessoa vê "Verifique seu e-mail", mas nada chega; o erro fica só no log do backend.

## Próximo passo

A definir. Opções naturais: trocar a senha estando logado (tela de perfil), ou começar o módulo de tenants e tema.
