package br.com.manageraccounting.modules.cliente.dto;

import java.time.Instant;

import br.com.manageraccounting.modules.cliente.RegimeTributario;
import br.com.manageraccounting.modules.cliente.TipoPessoa;

public record ClienteResponse(
        Long id,
        TipoPessoa tipoPessoa,
        String documento,
        String nome,
        String nomeFantasia,
        String email,
        String telefone,
        RegimeTributario regimeTributario,
        String observacoes,
        boolean ativo,
        Instant dataInclusao,
        Instant dataAtualizacao,
        long versao,
        AcessoAppResponse acessoApp) {
}
