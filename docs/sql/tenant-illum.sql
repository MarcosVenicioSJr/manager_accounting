-- Escritório ILLUM: tenant, cores e logo.
-- A logo já está no projeto em web/public/tenants/20911ebb-5ac2-4e81-866d-b79e1de891e4/logo.jpg,
-- por isso o tenant usa este GUID fixo. Pode rodar mais de uma vez (não duplica; atualiza o tema).
--
--   docker compose exec -T postgres psql -U manager -d manager_accounting < docs/sql/tenant-illum.sql
--
-- Cores tiradas da própria logo: vinho #730710 (fundo) e dourado #e1a34d (letras).

INSERT INTO tenant (nome_empresa, guid)
VALUES ('ILLUM Contabilidade', '20911ebb-5ac2-4e81-866d-b79e1de891e4')
ON CONFLICT (guid) DO NOTHING;

INSERT INTO tenant_tema (tenant_id, cor_primaria, cor_secundaria, logo_arquivo)
SELECT id, '#730710', '#e1a34d', 'logo.jpg'
FROM tenant
WHERE guid = '20911ebb-5ac2-4e81-866d-b79e1de891e4'
ON CONFLICT (tenant_id) DO UPDATE
SET cor_primaria = EXCLUDED.cor_primaria,
    cor_secundaria = EXCLUDED.cor_secundaria,
    logo_arquivo = EXCLUDED.logo_arquivo;

-- Se o ILLUM JÁ estava cadastrado com outro GUID, em vez de rodar o bloco acima, rode só isto
-- (troque o 999 pelo id do tenant) e depois rode o arquivo de novo para gravar o tema:
--
--   UPDATE tenant SET guid = '20911ebb-5ac2-4e81-866d-b79e1de891e4' WHERE id = 999;
--
-- Para entrar como ILLUM, cadastre um usuário nesse tenant (mesmo modelo do seed-dev.sql):
--
--   INSERT INTO usuario (tenant_id, login, senha_hash)
--   SELECT id, 'contador.illum', crypt('Temp@1234', gen_salt('bf', 10))
--   FROM tenant WHERE guid = '20911ebb-5ac2-4e81-866d-b79e1de891e4';
