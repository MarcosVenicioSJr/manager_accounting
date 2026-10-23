package br.com.manageraccounting.modules.cliente.dto;

import br.com.manageraccounting.modules.cliente.TipoPessoa;

/** Linha da lista de clientes. */
public record ClienteListaItem(
        Long id,
        TipoPessoa tipoPessoa,
        String documento,
        String nome,
        String nomeFantasia,
        String email,
        String telefone,
        String cidade,
        String uf,
        boolean ativo,
        boolean acessoAppAtivo) {
}
