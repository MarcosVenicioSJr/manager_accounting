package br.com.manageraccounting.modules.cliente;

/**
 * Dados do cliente já validados e normalizados pelo {@link ClienteService} (documento e telefone sem pontuação,
 * textos sem espaços nas pontas e vazios como nulos).
 */
record DadosCliente(
        TipoPessoa tipoPessoa,
        String documento,
        String nome,
        String nomeFantasia,
        String email,
        String telefone,
        RegimeTributario regimeTributario,
        String observacoes) {
}
