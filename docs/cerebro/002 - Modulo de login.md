---
projeto: manager_accounting
registro: 002
titulo: Módulo de login
data: 2026-10-15
status: em revisão (branch feature/login, ainda não validado no Maven)
modulo: autenticacao
commits:
  - 7a23399 - Módulo de login: JWT, refresh token, primeiro acesso e telas
  - 6a766be - Registro 002 no cérebro do projeto
  - Tela de login no visual aprovado (azul-marinho, foto da mesa) e Manter-me conectado
tags:
  - manager_accounting
  - registro
  - autenticacao
  - login
  - jwt
  - backend
  - frontend
---

# 002 - Módulo de login

Voltar para [[manager_accounting - Índice]] · Anterior: [[001 - Estrutura inicial do projeto]] · Próximo: [[003 - Recuperacao de senha]]

## Resumo

Primeira parte funcional do sistema: login do contador com JWT, sessão por refresh token, primeiro acesso (troca da senha temporária e dados adicionais), tela de login genérica e carregamento do tema do escritório após o login. A recuperação de senha por e-mail **não** entrou aqui (próxima etapa).

- **Branch:** `feature/login` (a `main` não foi alterada)
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/login

## Decisões

| Tema | Decisão |
|---|---|
| Access token | JWT HS256, validade de **4 horas**, claims só `userId` e `tenantId` (além de `iat`/`exp`) |
| Refresh token | Opaco (32 bytes aleatórios), validade fixa de **8 horas**, sem renovação (a sessão acaba em 8h) |
| Onde ficam os tokens | Access token só em memória no front; refresh token em cookie `HttpOnly` + `SameSite=Strict`, path `/api/auth` |
| Refresh no banco | Só o hash SHA-256 (`refresh_token.token_hash`); o logout revoga |
| Hash de senha | BCrypt |
| Senha temporária | Sem coluna `senha_temporaria`: a senha temporária é o próprio hash em `senha_hash` e é trocada no primeiro acesso |
| Pendência do primeiro acesso | Coluna `primeiro_acesso_concluido` (false até o usuário concluir) |
| Manter-me conectado | Com a opção marcada, o cookie de refresh sobrevive ao fechamento do navegador; sem ela, é cookie de sessão. A sessão no servidor continua valendo 8h nos dois casos |
| Dados do primeiro acesso | Nome completo, e-mail e telefone (telefone guardado só com dígitos, 10 a 13) |
| Proteção | 5 senhas erradas bloqueiam o usuário por 15 min; mensagem sempre genérica (não revela se o login existe) |
| Política de senha | Mínimo de 8 caracteres, com letras, números e símbolos; máximo de 72 bytes (limite do BCrypt); nova senha diferente da temporária |
| Tenant | Sempre do token, nunca do cliente; a cada requisição o filtro confere se usuário e tenant seguem ativos |

> [!note] Escolha minha, não definida por você
> O refresh token é **opaco e guardado em hash no banco** (e não um segundo JWT). Isso permite revogar no logout. Se preferir outro modelo, é só pedir.

## Banco (Flyway)

| Migration | Tabelas |
|---|---|
| `V1__tenant.sql` | `tenant` (id, nome_empresa, guid, ativo, data_inclusao) e `tenant_tema` (tenant_id, cor_primaria, cor_secundaria, logo_arquivo) |
| `V2__usuario.sql` | `usuario` (tenant_id, login único sem diferenciar maiúsculas, senha_hash, nome_completo, email, telefone, ativo, primeiro_acesso_concluido, tentativas_falhas, bloqueado_ate, data_inclusao) |
| `V3__refresh_token.sql` | `refresh_token` (usuario_id, token_hash, criado_em, expira_em, revogado_em) |

As três migrations foram executadas num PostgreSQL 16 de teste, junto com o script de cadastro, e as constraints (login duplicado, primeiro acesso sem dados, cor inválida) funcionaram.

## Endpoints

| Método e rota | Acesso | Resposta |
|---|---|---|
| `POST /api/auth/login` | Público | `{ accessToken, expiresIn, primeiroAcessoPendente }` + cookie `refresh_token` |
| `POST /api/auth/refresh` | Cookie | `{ accessToken, expiresIn, primeiroAcessoPendente }` |
| `POST /api/auth/logout` | Cookie | 204, revoga e apaga o cookie |
| `POST /api/auth/primeiro-acesso` | Bearer, só pendente | 204 |
| `GET /api/tenant/tema` | Bearer, concluído | `{ nomeEmpresa, logoUrl, corPrimaria, corSecundaria }` |

Enquanto o primeiro acesso não é concluído, as outras rotas respondem 403 com `codigo: PRIMEIRO_ACESSO_PENDENTE`.

## Visual da tela de login

Refeita no design aprovado na conversa "Visual do backoffice" (o artefato "Backoffice contábil"), sem logo nem cor de escritório:

- **Esquerda:** fundo azul-marinho com a foto da mesa (`web/public/images/login-hero.jpg`) sob um véu, a marca genérica "Backoffice contábil", o título "Mais controle para o seu negócio." e os três itens com ícone
- **Direita:** "Bem-vindo!", campos Usuário e Senha com ícone, botão de mostrar senha, "Manter-me conectado", "Esqueceu sua senha?", botão "Entrar", aviso de primeiro acesso e rodapé "Acesso restrito. Somente usuários autorizados."
- **Estados:** validação de campo vazio ao enviar, erro (alerta vermelho com a mensagem da API) e carregando ("Entrando…")
- **Cor de destaque:** a variável `--color-primary` (padrão `#1A6BDC`); o tema do contador a substitui depois do login
- **Celular (até 899px):** o painel com a foto some e a marca aparece compacta no topo
- **Fonte:** Figtree (Google Fonts, carregada no `index.html`)
- A tela de primeiro acesso usa o mesmo layout, e `/recuperar-senha` é só uma página reservada para o link "Esqueceu sua senha?" até o módulo de recuperação existir

## Backend

| Arquivo | O que faz |
|---|---|
| `core/security/JwtService` | Gera e valida o access token |
| `core/security/JwtAuthenticationFilter` | Lê o Bearer, confere usuário/tenant ativos no banco, preenche `SecurityContext` e `TenantContext` |
| `core/security/SecurityConfig` | Regras: login/refresh/logout públicos; primeiro-acesso só pendente; o resto só `ROLE_CONTADOR` |
| `core/security/RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` | 401 e 403 em JSON (ProblemDetail) |
| `core/security/JwtProperties` / `RefreshCookieProperties` | Configuração (segredo obrigatório, mínimo 32 caracteres) |
| `modules/auth/AuthService` | Login com bloqueio, refresh, logout e primeiro acesso |
| `modules/auth/RefreshTokenService` | Cria, valida e revoga sessões; limpeza diária de expirados |
| `modules/auth/PasswordPolicy` | Regras da senha |
| `modules/usuario/Usuario` + `UsuarioRepository` | Entidade e consultas (login sem diferenciar maiúsculas; usuário ativo com tenant ativo) |
| `modules/tenant/*` | Entidades, serviço e `GET /api/tenant/tema` |

## Front

| Arquivo | O que faz |
|---|---|
| `services/api.ts` | Axios com cookie, Bearer automático e renovação do token ao receber 401 (uma só requisição de refresh por vez) |
| `features/auth/AuthProvider` | Estado da sessão (`loading`, `anonymous`, `primeiro-acesso`, `authenticated`); recupera a sessão ao recarregar; carrega o tema após o login |
| `features/auth/ProtectedRoute` | Protege `/app` e `/primeiro-acesso` |
| `features/auth/AuthLayout` + `auth.css` + `icons.tsx` | Layout dividido aprovado: painel com foto à esquerda, formulário à direita |
| `pages/login/LoginPage` | Tela de login genérica (sem marca de escritório) |
| `pages/primeiro-acesso/PrimeiroAcessoPage` | Troca de senha com checklist da política + nome, e-mail e telefone |
| `pages/recuperar-senha/RecuperarSenhaPage` | Página reservada do link "Esqueceu sua senha?" (recuperação ainda não existe) |
| `pages/home/HomePage` | Mostra logo ou nome do tenant e o botão Sair |

## Configuração

| Variável | Padrão | Observação |
|---|---|---|
| `JWT_SECRET` | sem padrão | **Obrigatório em produção** (mínimo 32 caracteres). `mvn spring-boot:run` ativa o perfil `dev` com um segredo só de desenvolvimento |
| `JWT_ACCESS_TOKEN_TTL` | `4h` | |
| `JWT_REFRESH_TOKEN_TTL` | `8h` | |
| `REFRESH_COOKIE_SECURE` | `true` (`false` no perfil dev) | Em produção, HTTPS |
| `REFRESH_COOKIE_SAME_SITE` | `Strict` | Front e API precisam ser do mesmo site (ex.: subdomínios do mesmo domínio) |

## Como cadastrar um tenant e um usuário

Direto no banco, como combinado. O script `docs/sql/seed-dev.sql` cria um tenant de exemplo, o tema e o usuário `contador.exemplo` com a senha temporária `Temp@1234`, gerando o hash BCrypt com o `pgcrypto`:

```bash
docker compose exec -T postgres psql -U manager -d manager_accounting < docs/sql/seed-dev.sql
```

Para outro usuário, copie o bloco e troque os valores. A logo do escritório vai em `web/public/tenants/<guid>/<arquivo>` e o nome do arquivo em `tenant_tema.logo_arquivo`.

## Validação

- [x] Migrations e script de cadastro executados em PostgreSQL 16 (hash `$2a$`, constraints ok)
- [x] Front: `npm run build` (TypeScript estrito) e `oxlint` sem erros; política de senha do front conferida com casos de teste
- [x] Java: sem erros de sintaxe (checado com `javac`)
- [ ] **Backend compilar, subir e passar nos testes** (`mvn test`): não foi possível no ambiente de criação (o Maven Central é bloqueado lá). Rodar na sua máquina
- [x] Visual do login conferido no Chromium em 1440, 820, 390 e 360 px (sem rolagem horizontal), com a API simulada: estados padrão, validação, erro e carregando, rota "Esqueceu sua senha?" e redirecionamento para o primeiro acesso
- [ ] Teste manual do fluxo completo com o backend real (login → primeiro acesso → backoffice → recarregar página → sair)

## Pontos de atenção

- Se o `mvn spring-boot:run` falhar ao subir, o mais provável é detalhe de mapeamento JPA (`ddl-auto: validate`) ou de API de biblioteca, já que o código não foi compilado no ambiente de criação. Mande o log.
- A foto da mesa tem um quadro com o texto "Organização gera mais resultados" e etiquetas nas pastas; o véu azul esmaece o quadro, mas ele ainda aparece levemente. Se incomodar, troque o arquivo `login-hero.jpg`.
- A fonte Figtree vem do Google Fonts; sem internet o navegador usa a fonte padrão do sistema.
- **Limite por IP não foi feito**: só o bloqueio por login. Atrás de proxy/balanceador o IP visto pelo servidor é o do proxy, e um limite por IP bloquearia todo mundo. Definir junto com a hospedagem.
- Não há troca de senha pelo usuário logado ainda; a recuperação por e-mail está no [[003 - Recuperacao de senha]].
- Ao fazer um novo login, a sessão de refresh anterior continua válida até expirar (8h).

## Próximo passo

Recuperação de senha: feita no [[003 - Recuperacao de senha]].
