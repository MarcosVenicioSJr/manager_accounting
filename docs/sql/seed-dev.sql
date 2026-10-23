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
-- perfil ADMIN: o primeiro usuário de cada escritório é o administrador (convida os demais pelo backoffice)
INSERT INTO usuario (tenant_id, login, senha_hash, perfil)
SELECT id, 'contador.exemplo', crypt('Temp@1234', gen_salt('bf', 10)), 'ADMIN'
FROM novo_tenant;

-- Login: contador.exemplo   |   Senha temporária: Temp@1234

-- Clientes de exemplo do Escritório Exemplo (para ver a lista, a busca e a paginação funcionando).
-- CPFs e CNPJs abaixo são válidos só no cálculo; não pertencem a ninguém.
INSERT INTO cliente (tenant_id, tipo_pessoa, documento, nome, nome_fantasia, email, telefone, regime_tributario)
SELECT u.tenant_id, c.tipo_pessoa, c.documento, c.nome, c.nome_fantasia, c.email, c.telefone, c.regime_tributario
FROM usuario u
CROSS JOIN (VALUES
    ('JURIDICA', '11222333000181', 'Padaria Pão Quente LTDA', 'Pão Quente', 'contato@paoquente.com.br', '85988887777', 'SIMPLES_NACIONAL'),
    ('JURIDICA', '12ABC34501DE35', 'Alfa Tecnologia S.A.', 'Alfa Tech', 'financeiro@alfatech.com.br', '8532221100', 'LUCRO_REAL'),
    ('FISICA',   '52998224725',    'Maria Aparecida Souza', NULL, 'maria.souza@email.com', '85999990000', NULL),
    ('JURIDICA', '45723174000110', 'Oficina do João ME', 'Oficina do João', NULL, '85987654321', 'MEI'),
    ('FISICA',   '11144477735',    'Carlos Eduardo Lima', NULL, NULL, NULL, NULL)
) AS c (tipo_pessoa, documento, nome, nome_fantasia, email, telefone, regime_tributario)
WHERE lower(u.login) = 'contador.exemplo'
ON CONFLICT (tenant_id, documento) DO NOTHING;
