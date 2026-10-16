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

# 3. Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

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

## Documentação

- [Arquitetura e decisões](docs/arquitetura.md)
- [Cérebro do projeto (notas para o Obsidian)](docs/cerebro/)
