-- Clientes: endereço (opcional, preenchido pela busca de CEP no front) e busca sem diferenciar acentos.

ALTER TABLE cliente
    ADD COLUMN cep         VARCHAR(8),
    ADD COLUMN logradouro  VARCHAR(150),
    ADD COLUMN numero      VARCHAR(20),
    ADD COLUMN complemento VARCHAR(100),
    ADD COLUMN bairro      VARCHAR(100),
    ADD COLUMN cidade      VARCHAR(100),
    ADD COLUMN uf          VARCHAR(2),
    ADD CONSTRAINT ck_cliente_cep CHECK (cep ~ '^[0-9]{8}$'),
    ADD CONSTRAINT ck_cliente_uf CHECK (uf IN ('AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS',
        'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'));

-- Texto de busca: nome, nome fantasia e e-mail em minúsculas e sem acentos ("João" é encontrado por "joao").
-- Daqui em diante o backend preenche esta coluna a cada gravação; aqui só os clientes que já existem.
-- (translate em vez da extensão unaccent, para não exigir permissão de superusuário no banco.)
ALTER TABLE cliente ADD COLUMN busca VARCHAR(500) NOT NULL DEFAULT '';

UPDATE cliente
SET busca = lower(translate(
        concat_ws(' ', nome, nome_fantasia, email),
        'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ',
        'AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn'));
