# manager_accounting

Backoffice multi-tenant para contadores organizarem seus clientes. Cada escritório (tenant)
acessa apenas os próprios dados, com logo e cores carregados após o login.

## Stack

- **Backend:** Spring Boot 3, Java 21, Spring Security, JPA, Flyway, PostgreSQL
- **Web:** React + TypeScript (Vite), React Router, Axios
- **Mobile:** React Native (etapa futura)
- **Infra local:** Docker Compose com PostgreSQL e Mailpit

## Pré-requisitos

- Docker e Docker Compose
- Java 21 e Maven
- Node.js 20+

## Como rodar

```bash
# 1. Variáveis de ambiente
cp .env.example .env

# 2. Banco e servidor de e-mail
docker compose up -d

# 3. Backend (http://localhost:8080) - "mvn spring-boot:run" já ativa o perfil dev (segredo JWT local)
cd backend
mvn spring-boot:run

# 3.1. Usuário de exemplo (depois do backend subir uma vez, para o Flyway criar as tabelas)
docker compose exec -T postgres psql -U manager -d manager_accounting < docs/sql/seed-dev.sql
# login: contador.exemplo | senha temporária: Temp@1234 (a troca é exigida no primeiro acesso)

# 4. Web (http://localhost:5173)
cd web
cp .env.example .env
npm install
npm run dev
```

| Serviço | Endereço |
|---|---|
| Web | http://localhost:5173 |
| API | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |
| Mailpit (e-mails de teste) | http://localhost:8025 |
| PostgreSQL | localhost:5432 |

## Login

- Telas: `/login` (genérica) e `/primeiro-acesso` (troca da senha temporária + nome, e-mail e telefone)
- A foto do lado esquerdo do login é o arquivo `web/public/images/login-hero.jpg` (sem ele aparece só o degradê)
- A logo de cada escritório fica em `web/public/tenants/<guid>/<arquivo>` e o nome do arquivo em `tenant_tema.logo_arquivo`
- Como cadastrar tenants e usuários: [docs/sql/seed-dev.sql](docs/sql/seed-dev.sql)

## Documentação

- [Arquitetura e decisões](docs/arquitetura.md)
- [Cérebro do projeto (notas para o Obsidian)](docs/cerebro/)
