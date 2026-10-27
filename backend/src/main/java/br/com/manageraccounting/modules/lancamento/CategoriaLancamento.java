package br.com.manageraccounting.modules.lancamento;

/**
 * Categorias fixas, iguais para todos os escritórios. Para incluir uma nova é preciso mexer aqui, na constraint
 * {@code ck_lancamento_categoria} (nova migration) e nada mais: o front e o app leem a lista de
 * {@code GET /api/lancamentos/categorias}. A ordem aqui é a ordem em que aparecem na tela ("Outros" por último).
 */
public enum CategoriaLancamento {
    ALIMENTACAO("Alimentação"),
    ALUGUEL("Aluguel"),
    ENERGIA_AGUA("Energia e água"),
    INTERNET_TELEFONE("Internet e telefone"),
    COMBUSTIVEL_TRANSPORTE("Combustível e transporte"),
    MERCADORIAS_FORNECEDORES("Mercadorias e fornecedores"),
    MATERIAL_ESCRITORIO("Material de escritório"),
    EQUIPAMENTOS("Equipamentos"),
    MANUTENCAO("Manutenção"),
    SERVICOS_TERCEIROS("Serviços de terceiros"),
    FOLHA_PRO_LABORE("Folha e pró-labore"),
    IMPOSTOS_TAXAS("Impostos e taxas"),
    TARIFAS_BANCARIAS("Tarifas bancárias"),
    EMPRESTIMOS_FINANCIAMENTOS("Empréstimos e financiamentos"),
    CARTAO_CREDITO("Cartão de crédito"),
    MARKETING("Marketing"),
    SAUDE("Saúde"),
    EDUCACAO("Educação"),
    OUTROS("Outros");

    private final String rotulo;

    CategoriaLancamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
