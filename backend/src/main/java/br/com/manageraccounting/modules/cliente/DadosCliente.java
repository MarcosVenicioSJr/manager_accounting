package br.com.manageraccounting.modules.cliente;

import br.com.manageraccounting.modules.cliente.dto.EnderecoDto;

/**
 * Dados do cliente já validados e normalizados pelo {@link ClienteService} (documento, telefone e CEP sem
 * pontuação, textos sem espaços nas pontas e vazios como nulos). {@code endereco} nunca é nulo (campos podem ser).
 */
record DadosCliente(
        TipoPessoa tipoPessoa,
        String documento,
        String nome,
        String nomeFantasia,
        String email,
        String telefone,
        RegimeTributario regimeTributario,
        String observacoes,
        EnderecoDto endereco) {
}
