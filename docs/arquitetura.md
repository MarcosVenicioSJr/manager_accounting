# Arquitetura — manager_accounting

Backoffice multi-tenant (white-label) para contadores organizarem seus clientes.

## Decisões

| Tema | Decisão |
|---|---|
| Multi-tenancy | Banco único (PostgreSQL), dados do contador separados por `tenant_id`; tabelas de referência globais ficam sem tenant |
| Origem do tenant | Sempre do token JWT validado no servidor, nunca de URL, body ou header livre |
| Login | Tela genérica, sem marca de escritório; login único na plataforma inteira |
| Tema | Logo e cores do tenant carregados após o login |
| Tenant | `Id`, `NomeEmpresa`, `Guid` (código da empresa, único e imutável), `Ativo`, `DataInclusao` |
| Usuário | Entidade separada do tenant (1 tenant para N usuários); perfis ADMIN (gerencia usuários, desativa clientes, revoga acesso ao app) e USUARIO (cadastra/edita clientes e gera token) |
| Cadastro | Tenants e o primeiro usuário (ADMIN) de cada um cadastrados direto no banco, com senha temporária; os demais usuários são convidados pelo administrador (link por e-mail para criar a senha) |
| Tema e logo | Não são alterados pelo backoffice: cores em `tenant_tema` (banco) e logo em `web/public/tenants/<guid>/` |
| Primeiro acesso | Troca de senha obrigatória e preenchimento de dados adicionais (telefone, e-mail etc.) |
| Troca de senha/e-mail logado | Exige a senha atual e só vale depois da confirmação pelo link do e-mail (senha: e-mail atual; e-mail: novo endereço) |
| Recuperação de senha | Usuário digita o login, recebe por e-mail um token de confirmação e, com o token válido, define a nova senha |
| Política de senha | Mínimo de 8 caracteres, com letras, números e símbolos |
| E-mail | Mailpit no Docker em desenvolvimento; provedor SMTP de produção definido na publicação |
| Access token | JWT HS256 de 4 horas, só com `userId` e `tenantId` (além de iat/exp); fica em memória no front |
| Refresh token | Opaco, 8 horas fixas (sem renovação), em cookie `HttpOnly` + `SameSite=Strict` restrito a `/api/auth`; no banco só o hash SHA-256 |
| Senha | BCrypt; a senha temporária é guardada em `senha_hash` e substituída no primeiro acesso (sem coluna de senha temporária) |
| Proteção do login | 5 senhas erradas bloqueiam o usuário por 15 minutos; mensagem de erro sempre genérica |
| Tema no backoffice | Cores do tenant passam por um ajuste de contraste (WCAG AA) antes de virar variáveis CSS; menu lateral na primária escurecida |
| Clientes | Isolados por `tenant_id`; CPF/CNPJ único por tenant (aceita CNPJ alfanumérico); edição com controle de versão; desativação em vez de exclusão |
| Token do app do cliente | 16 caracteres Base32 Crockford (`XXXX-XXXX-XXXX-XXXX`), só o hash SHA-256 no banco, um ativo por cliente, mostrado uma única vez |

## Endpoints de autenticação

| Método e rota | Acesso | Resposta |
|---|---|---|
| `POST /api/auth/login` | Público | `{ accessToken, expiresIn, primeiroAcessoPendente }` + cookie `refresh_token` |
| `POST /api/auth/refresh` | Cookie de refresh | `{ accessToken, expiresIn, primeiroAcessoPendente }` |
| `POST /api/auth/logout` | Cookie de refresh | 204, revoga a sessão e apaga o cookie |
| `POST /api/auth/primeiro-acesso` | Bearer, só com primeiro acesso pendente | 204 |
| `GET /api/tenant/tema` | Bearer, primeiro acesso concluído | `{ nomeEmpresa, logoUrl, corPrimaria, corSecundaria }` |

## Endpoints do backoffice

| Método e rota | Resposta |
|---|---|
| `GET /api/usuario/me` | `{ login, nomeCompleto, email }` do contador logado |
| `GET /api/clientes?busca=&status=ATIVOS\|INATIVOS\|TODOS&pagina=0&tamanho=20` | Página de clientes do tenant |
| `GET /api/clientes/resumo` | `{ ativos, inativos, comAcessoApp }` |
| `GET /api/clientes/{id}` | Cliente + situação do acesso ao app |
| `POST /api/clientes` · `PUT /api/clientes/{id}` | Cadastro e edição (409 `DOCUMENTO_DUPLICADO` / `EDICAO_SIMULTANEA`) |
| `POST /api/clientes/{id}/desativar` · `/reativar` | 204 |
| `POST /api/clientes/{id}/acesso-app` · `DELETE` | Gera (token só nesta resposta) ou revoga o token do app |

| `GET`/`PUT /api/perfil` | Meu perfil (nome e telefone na hora) |
| `POST /api/perfil/senha` · `/email` | Pede a troca; só vale depois do link do e-mail |
| `POST /api/auth/confirmar-alteracao` (público) | Confirma a troca pelo token do link |

| `GET/POST /api/usuarios`, `/api/usuarios/{id}/...` (ADMIN) | Usuários do escritório: convite, perfil, desativar/reativar |
| `POST /api/auth/convite/aceitar` (público) | Convidado cria a senha pelo link |

Cliente de outro tenant responde 404, igual a um id inexistente. Detalhes em `docs/cerebro/005 - Clientes.md`.

Enquanto o primeiro acesso não é concluído, qualquer outra rota responde 403 com `codigo: PRIMEIRO_ACESSO_PENDENTE`.
A cada requisição o filtro JWT confere no banco se o usuário e o tenant continuam ativos.

## Estrutura

```
manager_accounting/
├── backend/                Spring Boot 3 (Java 21)
│   └── src/main/java/br/com/manageraccounting/
│       ├── core/
│       │   ├── config/     CORS e configurações gerais
│       │   ├── security/   Spring Security (stateless) e, depois, o filtro JWT
│       │   ├── tenant/     TenantContext (tenant da requisição)
│       │   └── exception/  Tratamento global de erros
│       └── modules/
│           ├── auth/       Login, primeiro acesso, recuperação de senha
│           ├── tenant/     Escritórios e tema
│           ├── usuario/    Contadores
│           └── cliente/    Clientes do contador e token do app
├── web/                    React + TypeScript (Vite)
│   └── src/
│       ├── app/            Componente raiz
│       ├── routes/         Rotas
│       ├── layouts/        Layout do backoffice (menu lateral e cabeçalho)
│       ├── pages/          Telas (login, início, clientes)
│       ├── components/     Ícones e componentes base (diálogo, avisos)
│       ├── features/       Lógica por módulo
│       ├── services/       Cliente HTTP
│       ├── theme/          Tema do tenant
│       └── hooks/          Hooks compartilhados
├── mobile/                 App do cliente (React Native), etapa futura
├── docs/                   Documentação
└── docker-compose.yml      PostgreSQL + Mailpit
```
