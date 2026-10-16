---
projeto: manager_accounting
registro: 001
titulo: Estrutura inicial do projeto
data: 2026-10-15
status: concluído
modulo: estrutura
commits:
  - 5451737 - Estrutura inicial do projeto manager_accounting
  - Registro 001 no cérebro do projeto e remoção do App.tsx do template
tags:
  - manager_accounting
  - registro
  - estrutura
  - backend
  - frontend
  - docker
---

# 001 - Estrutura inicial do projeto

Voltar para [[manager_accounting - Índice]]

## Resumo

Criação do esqueleto do **manager_accounting**: monorepo com backend (Spring Boot), web (React + TypeScript), pasta reservada para o app mobile, infraestrutura local em Docker (PostgreSQL + Mailpit) e documentação de arquitetura. Ainda não existe regra de negócio, apenas a base onde os módulos serão construídos.

- **Repositório:** https://github.com/MarcosVenicioSJr/manager_accounting
- **Branch:** `main`

## Contexto e decisões que guiaram a estrutura

| Tema | Decisão |
|---|---|
| Produto | Backoffice multi-tenant (white-label) para contadores organizarem seus clientes |
| Multi-tenancy | Banco único (PostgreSQL), dados do contador separados por `tenant_id`; tabelas de referência globais sem tenant |
| Origem do tenant | Sempre do token JWT validado no servidor, nunca de URL, body ou header livre |
| Login | Tela genérica, sem marca de escritório; login único na plataforma inteira |
| Tema | Logo e cores do tenant carregados após o login |
| Tenant | `Id`, `NomeEmpresa`, `Guid` (código da empresa, único e imutável), `Ativo`, `DataInclusao` |
| Usuário | Entidade separada do tenant (1 tenant para N usuários); por enquanto só o contador acessa |
| Cadastro | Tenants e usuários cadastrados direto no banco, com senha temporária |
| Primeiro acesso | Troca de senha obrigatória e preenchimento de dados adicionais |
| Recuperação de senha | Login → token de confirmação por e-mail → nova senha |
| Política de senha | Mínimo de 8 caracteres, com letras, números e símbolos |
| E-mail | Mailpit no Docker em desenvolvimento; provedor de produção definido na publicação |
| Stack | Spring Boot 3.5.6 + Java 21, React + TypeScript (Vite), React Native (futuro) |

> [!note] Nome
> O nome do projeto é **manager_accounting**. ILLUM é o nome de um escritório (um possível tenant), não da plataforma, e não aparece nas telas do sistema.

## Estrutura de pastas

```
manager_accounting/
├── backend/                Spring Boot 3.5 (Java 21)
│   ├── pom.xml
│   └── src/main/
│       ├── java/br/com/manageraccounting/
│       │   ├── ManagerAccountingApplication.java
│       │   ├── core/
│       │   │   ├── config/      CorsConfig
│       │   │   ├── security/    SecurityConfig
│       │   │   ├── tenant/      TenantContext
│       │   │   └── exception/   GlobalExceptionHandler
│       │   └── modules/
│       │       ├── auth/        (vazio, só package-info)
│       │       ├── tenant/      (vazio, só package-info)
│       │       └── usuario/     (vazio, só package-info)
│       └── resources/
│           ├── application.yml
│           └── db/migration/    (Flyway, ainda sem migrations)
├── web/                    React + TypeScript (Vite)
│   └── src/
│       ├── main.tsx
│       ├── index.css
│       ├── app/App.tsx
│       ├── routes/router.tsx
│       ├── pages/login/LoginPage.tsx
│       ├── pages/home/HomePage.tsx
│       ├── theme/               theme.ts, ThemeContext.ts, ThemeProvider.tsx
│       ├── hooks/useTenantTheme.ts
│       ├── services/api.ts
│       ├── components/  features/auth/  types/  assets/images/   (vazias)
├── mobile/                 Reservado para o app do cliente (React Native)
├── docs/
│   ├── arquitetura.md
│   └── cerebro/            Notas deste cérebro (Obsidian)
├── docker-compose.yml      PostgreSQL 17 + Mailpit
├── .env.example
├── .editorconfig
├── .gitignore
└── README.md
```

## Backend

### Dependências (`backend/pom.xml`)

| Dependência | Para quê |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-security` | Autenticação e autorização |
| `spring-boot-starter-data-jpa` | Persistência (Hibernate) |
| `spring-boot-starter-validation` | Validação de entrada (`@Valid`) |
| `spring-boot-starter-mail` | Envio do token de recuperação de senha |
| `spring-boot-starter-actuator` | Health check |
| `flyway-core` + `flyway-database-postgresql` | Migrations do banco |
| `postgresql` | Driver JDBC |
| `spring-boot-starter-test` + `spring-security-test` | Testes |

Pacote base: `br.com.manageraccounting` · Java 21 · Spring Boot 3.5.6.

### Classes criadas

| Arquivo | O que faz |
|---|---|
| `ManagerAccountingApplication` | Classe de inicialização |
| `core/security/SecurityConfig` | API stateless, CSRF desligado, CORS ligado. Libera `/actuator/health` e `/api/auth/**`; o resto exige autenticação. Define o `PasswordEncoder` (BCrypt) |
| `core/config/CorsConfig` | Origens permitidas vindas de `app.cors.allowed-origins`; métodos GET, POST, PUT, PATCH, DELETE, OPTIONS; headers `Authorization` e `Content-Type` |
| `core/tenant/TenantContext` | `ThreadLocal<Long>` com o tenant da requisição (`setTenantId`, `getTenantId`, `clear`). Será preenchido pelo filtro JWT |
| `core/exception/GlobalExceptionHandler` | Erros no formato `ProblemDetail` (RFC 9457); validação retorna 400 com o erro de cada campo |
| `modules/*/package-info.java` | Descrevem o papel dos módulos `auth`, `tenant` e `usuario` |

### Configuração (`application.yml`)

| Chave | Valor padrão | Variável de ambiente |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/manager_accounting` | `DB_URL` |
| `spring.datasource.username` / `password` | `manager` / `manager` | `DB_USERNAME` / `DB_PASSWORD` |
| `spring.jpa.hibernate.ddl-auto` | `validate` (o schema vem só do Flyway) | — |
| `spring.jpa.open-in-view` | `false` | — |
| `spring.flyway.locations` | `classpath:db/migration` | — |
| `spring.mail.host` / `port` | `localhost` / `1025` (Mailpit) | `MAIL_HOST` / `MAIL_PORT` |
| `server.port` | `8080` | `SERVER_PORT` |
| `management.endpoints.web.exposure.include` | `health` | — |
| `app.cors.allowed-origins` | `http://localhost:5173` | `CORS_ALLOWED_ORIGINS` |
| `app.mail.from` | `no-reply@manageraccounting.local` | `MAIL_FROM` |

## Web

| Arquivo | O que faz |
|---|---|
| `main.tsx` | Monta o `App` dentro do `StrictMode` |
| `app/App.tsx` | Envolve as rotas com o `ThemeProvider` |
| `routes/router.tsx` | `/` redireciona para `/login`; `/login` (tela genérica); `/app` (backoffice, ainda sem proteção) |
| `pages/login/LoginPage.tsx` | Placeholder da tela de login (layout aprovado: foto à esquerda, formulário à direita) |
| `pages/home/HomePage.tsx` | Placeholder da página inicial do backoffice |
| `theme/theme.ts` | Tipo `TenantTheme` (`tenantName`, `logoUrl`, `primaryColor`, `secondaryColor`) e tema padrão neutro |
| `theme/ThemeContext.ts` | Contexto React do tema |
| `theme/ThemeProvider.tsx` | Guarda o tema atual e aplica as cores como variáveis CSS (`--color-primary`, `--color-secondary`) |
| `hooks/useTenantTheme.ts` | Hook para ler e trocar o tema |
| `services/api.ts` | Instância do Axios com `baseURL` de `VITE_API_URL` (padrão `http://localhost:8080/api`) |
| `.env.example` | `VITE_API_URL` |

Dependências adicionadas além do template: `react-router-dom` e `axios`. O título da página é "Manager Accounting" e o idioma é `pt-BR`.

## Docker (`docker-compose.yml`)

| Serviço | Imagem | Portas | Observação |
|---|---|---|---|
| `postgres` | `postgres:17-alpine` | `5432` | Volume `postgres_data`, health check com `pg_isready` |
| `mailpit` | `axllent/mailpit:latest` | `1025` (SMTP), `8025` (web) | Captura os e-mails enviados pelo backend |

## Como rodar

```bash
cp .env.example .env
docker compose up -d

cd backend
mvn spring-boot:run          # http://localhost:8080

cd web
cp .env.example .env
npm install
npm run dev                  # http://localhost:5173
```

| Serviço | Endereço |
|---|---|
| Web | http://localhost:5173 |
| API | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |
| Mailpit | http://localhost:8025 |

## Validação

- [x] Web compila (`npm run build`) e passa no lint (`oxlint`)
- [ ] Backend compilar e subir (`mvn spring-boot:run`) — não foi possível testar no ambiente de criação, falta validar na máquina local

## Pontos de atenção

- O Spring Boot não lê o arquivo `.env` sozinho. Os valores padrão do `application.yml` já batem com o `docker-compose.yml`; para mudar, exporte as variáveis ou configure na IDE.
- Com o Spring Security ativo e sem filtro JWT ainda, qualquer endpoint fora de `/actuator/health` e `/api/auth/**` responde 401.
- A versão do Spring Boot está fixada em 3.5.6. Migrar para o Spring Boot 4 muda a configuração de alguns starters; a decisão deve ser tomada antes do módulo de autenticação.

## Próximo passo

Módulo de autenticação: migrations de tenant, tema e usuário; login com JWT e `tenant_id` no token; primeiro acesso; recuperação de senha pelo Mailpit; tela de login dividida.
