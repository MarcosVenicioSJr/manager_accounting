-- Cadastro de exemplo para DESENVOLVIMENTO: um tenant, o tema dele e um usuário com senha temporária.
-- NÃO faz parte das migrations (o Flyway não executa este arquivo). Rode manualmente depois de subir o backend
-- uma vez (para o Flyway criar as tabelas):
--
--   docker compose exec -T postgres psql -U manager -d manager_accounting < docs/sql/seed-dev.sql
--
-- A senha temporária é guardada em senha_hash como hash BCrypt, gerado aqui pelo pgcrypto.
-- O prefixo $2a$ gerado pelo pgcrypto é aceito pelo BCryptPasswordEncoder do Spring Security.
-- No primeiro acesso o usuário troca essa senha e preenche nome completo, e-mail e telefone.
--
-- Para cadastrar outro usuário/tenant, copie o bloco e troque os valores.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH novo_tenant AS (
    INSERT INTO tenant (nome_empresa)
    VALUES ('Escritório Exemplo')
    RETURNING id
),
novo_tema AS (
    INSERT INTO tenant_tema (tenant_id, cor_primaria, cor_secundaria)
    SELECT id, '#0f766e', '#115e59' FROM novo_tenant
)
INSERT INTO usuario (tenant_id, login, senha_hash)
SELECT id, 'contador.exemplo', crypt('Temp@1234', gen_salt('bf', 10))
FROM novo_tenant;

-- Login: contador.exemplo   |   Senha temporária: Temp@1234
