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
| Usuário | Entidade separada do tenant (1 tenant para N usuários); por enquanto só o contador acessa |
| Cadastro | Tenants e usuários cadastrados direto no banco, com senha temporária |
| Primeiro acesso | Troca de senha obrigatória e preenchimento de dados adicionais (telefone, e-mail etc.) |
| Recuperação de senha | Usuário digita o login, recebe por e-mail um token de confirmação e, com o token válido, define a nova senha |
| Política de senha | Mínimo de 8 caracteres, com letras, números e símbolos |
| E-mail | Mailpit no Docker em desenvolvimento; provedor SMTP de produção definido na publicação |
| Access token | JWT HS256 de 4 horas, só com `userId` e `tenantId` (além de iat/exp); fica em memória no front |
| Refresh token | Opaco, 8 horas fixas (sem renovação), em cookie `HttpOnly` + `SameSite=Strict` restrito a `/api/auth`; no banco só o hash SHA-256 |
| Senha | BCrypt; a senha temporária é guardada em `senha_hash` e substituída no primeiro acesso (sem coluna de senha temporária) |
| Proteção do login | 5 senhas erradas bloqueiam o usuário por 15 minutos; mensagem de erro sempre genérica |

## Endpoints de autenticação

| Método e rota | Acesso | Resposta |
|---|---|---|
| `POST /api/auth/login` | Público | `{ accessToken, expiresIn, primeiroAcessoPendente }` + cookie `refresh_token` |
| `POST /api/auth/refresh` | Cookie de refresh | `{ accessToken, expiresIn, primeiroAcessoPendente }` |
| `POST /api/auth/logout` | Cookie de refresh | 204, revoga a sessão e apaga o cookie |
| `POST /api/auth/primeiro-acesso` | Bearer, só com primeiro acesso pendente | 204 |
| `GET /api/tenant/tema` | Bearer, primeiro acesso concluído | `{ nomeEmpresa, logoUrl, corPrimaria, corSecundaria }` |

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
│           └── usuario/    Contadores
├── web/                    React + TypeScript (Vite)
│   └── src/
│       ├── app/            Componente raiz
│       ├── routes/         Rotas
│       ├── pages/          Telas (login, home)
│       ├── components/     Componentes compartilhados
│       ├── features/       Lógica por módulo
│       ├── services/       Cliente HTTP
│       ├── theme/          Tema do tenant
│       └── hooks/          Hooks compartilhados
├── mobile/                 App do cliente (React Native), etapa futura
├── docs/                   Documentação
└── docker-compose.yml      PostgreSQL + Mailpit
```
