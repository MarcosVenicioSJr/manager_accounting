-- Encerramento de sessões ao trocar a senha.
-- O refresh token já era revogado, mas o access token (JWT, 4h) continuava valendo até expirar.
-- Tokens emitidos antes deste instante são recusados pelo filtro JWT. Nulo: nenhum encerramento ainda.
-- Guardado com precisão de segundos, a mesma do "iat" do JWT.

ALTER TABLE usuario ADD COLUMN sessoes_validas_desde TIMESTAMPTZ;
