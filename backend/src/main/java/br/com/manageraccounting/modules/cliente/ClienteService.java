package br.com.manageraccounting.modules.cliente;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manageraccounting.core.exception.BusinessException;
import br.com.manageraccounting.core.exception.ConflictException;
import br.com.manageraccounting.core.exception.NotFoundException;
import br.com.manageraccounting.modules.cliente.dto.AcessoAppResponse;
import br.com.manageraccounting.modules.cliente.dto.ClienteListaItem;
import br.com.manageraccounting.modules.cliente.dto.ClienteRequest;
import br.com.manageraccounting.modules.cliente.dto.ClienteResponse;
import br.com.manageraccounting.modules.cliente.dto.ClientesResumoResponse;
import br.com.manageraccounting.modules.cliente.dto.EnderecoDto;
import br.com.manageraccounting.modules.cliente.dto.PaginaResponse;
import br.com.manageraccounting.modules.cliente.dto.TokenGeradoResponse;
import jakarta.persistence.criteria.Predicate;

/**
 * Regras do cadastro de clientes. Todo método recebe o tenant do contador logado (vindo do token) e só enxerga
 * clientes desse tenant: cliente de outro escritório responde "não encontrado", igual a um id que não existe.
 */
@Service
public class ClienteService {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;
    static final String CODIGO_DUPLICADO = "DOCUMENTO_DUPLICADO";
    static final String CODIGO_EDICAO_SIMULTANEA = "EDICAO_SIMULTANEA";
    static final String MSG_NAO_ENCONTRADO = "Cliente não encontrado.";
    static final String MSG_DUPLICADO = "Já existe um cliente com este CPF/CNPJ no seu escritório.";
    static final String MSG_EDICAO_SIMULTANEA =
            "Este cliente foi alterado por outra pessoa depois que você abriu a tela. Recarregue para ver a versão atual.";

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository clienteRepository;
    private final ClienteTokenAcessoRepository tokenRepository;
    private final Clock clock;

    public ClienteService(ClienteRepository clienteRepository, ClienteTokenAcessoRepository tokenRepository,
            Clock clock) {
        this.clienteRepository = clienteRepository;
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }

    // ---------- consulta ----------

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteListaItem> listar(Long tenantId, String busca, StatusFiltro status,
            int pagina, int tamanho) {
        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        Sort ordem = Sort.by(Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("id"));

        Page<Cliente> page = clienteRepository.findAll(
                filtro(tenantId, busca, status == null ? StatusFiltro.ATIVOS : status),
                PageRequest.of(paginaValida, tamanhoValido, ordem));

        Set<Long> comToken = page.isEmpty()
                ? Set.of()
                : new HashSet<>(tokenRepository.findClienteIdsComTokenAtivo(
                        page.getContent().stream().map(Cliente::getId).toList()));

        List<ClienteListaItem> itens = page.getContent().stream()
                .map(c -> new ClienteListaItem(c.getId(), c.getTipoPessoa(), c.getDocumento(), c.getNome(),
                        c.getNomeFantasia(), c.getEmail(), c.getTelefone(), c.getCidade(), c.getUf(), c.isAtivo(),
                        comToken.contains(c.getId())))
                .toList();

        return new PaginaResponse<>(itens, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ClienteResponse obter(Long tenantId, Long id) {
        return toResponse(buscar(tenantId, id));
    }

    @Transactional(readOnly = true)
    public ClientesResumoResponse resumo(Long tenantId) {
        return new ClientesResumoResponse(
                clienteRepository.countByTenantIdAndAtivo(tenantId, true),
                clienteRepository.countByTenantIdAndAtivo(tenantId, false),
                tokenRepository.countAtivosDoTenant(tenantId));
    }

    // ---------- cadastro ----------

    @Transactional
    public ClienteResponse criar(Long tenantId, ClienteRequest request) {
        DadosCliente dados = normalizar(request);
        if (clienteRepository.existsByTenantIdAndDocumento(tenantId, dados.documento())) {
            throw new ConflictException(CODIGO_DUPLICADO, MSG_DUPLICADO);
        }
        Cliente cliente = new Cliente(tenantId, dados, clock.instant());
        salvar(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse atualizar(Long tenantId, Long id, ClienteRequest request) {
        if (request.versao() == null) {
            throw new BusinessException("Informe a versão do cliente que está sendo editada.");
        }
        Cliente cliente = buscar(tenantId, id);
        if (cliente.getVersao() != request.versao()) {
            throw new ConflictException(CODIGO_EDICAO_SIMULTANEA, MSG_EDICAO_SIMULTANEA);
        }
        DadosCliente dados = normalizar(request);
        if (clienteRepository.existsByTenantIdAndDocumentoAndIdNot(tenantId, dados.documento(), id)) {
            throw new ConflictException(CODIGO_DUPLICADO, MSG_DUPLICADO);
        }
        cliente.atualizar(dados, clock.instant());
        salvar(cliente);
        return toResponse(cliente);
    }

    /** Desativa o cliente e revoga o token do app. Chamar de novo em um cliente já inativo não muda nada. */
    @Transactional
    public void desativar(Long tenantId, Long usuarioId, Long id) {
        Cliente cliente = buscar(tenantId, id);
        if (!cliente.isAtivo()) {
            return;
        }
        Instant agora = clock.instant();
        cliente.desativar(agora);
        salvar(cliente);
        if (tokenRepository.revogarAtivos(id, agora) > 0) {
            log.info("Token do app revogado ao desativar cliente: tenant={} cliente={} usuario={}",
                    tenantId, id, usuarioId);
        }
    }

    /** Reativa o cliente. O token do app não volta: o contador gera um novo se o cliente precisar. */
    @Transactional
    public void reativar(Long tenantId, Long id) {
        Cliente cliente = buscar(tenantId, id);
        if (cliente.isAtivo()) {
            return;
        }
        cliente.reativar(clock.instant());
        salvar(cliente);
    }

    // ---------- token do app ----------

    /**
     * Gera o token de acesso ao app e revoga o anterior, se houver. O valor só existe nesta resposta.
     * A linha do cliente fica travada até o fim da transação, para dois pedidos ao mesmo tempo não deixarem
     * dois tokens ativos.
     */
    @Transactional
    public TokenGeradoResponse gerarToken(Long tenantId, Long usuarioId, Long id) {
        Cliente cliente = clienteRepository.findForUpdate(id, tenantId)
                .orElseThrow(() -> new NotFoundException(MSG_NAO_ENCONTRADO));
        if (!cliente.isAtivo()) {
            throw new BusinessException("Reative o cliente antes de gerar o acesso ao app.");
        }

        Instant agora = clock.instant();
        tokenRepository.revogarAtivos(id, agora);
        String token = TokenAcessoApp.gerar();
        tokenRepository.saveAndFlush(new ClienteTokenAcesso(id, TokenAcessoApp.hash(token), usuarioId, agora));

        log.info("Token do app gerado: tenant={} cliente={} usuario={}", tenantId, id, usuarioId);
        return new TokenGeradoResponse(token, agora);
    }

    /** Revoga o token do app; o cliente perde o acesso. Sem token ativo, não muda nada. */
    @Transactional
    public void revogarToken(Long tenantId, Long usuarioId, Long id) {
        buscar(tenantId, id);
        if (tokenRepository.revogarAtivos(id, clock.instant()) > 0) {
            log.info("Token do app revogado: tenant={} cliente={} usuario={}", tenantId, id, usuarioId);
        }
    }

    // ---------- apoio ----------

    private Cliente buscar(Long tenantId, Long id) {
        return clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException(MSG_NAO_ENCONTRADO));
    }

    private void salvar(Cliente cliente) {
        try {
            clienteRepository.saveAndFlush(cliente);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException(CODIGO_EDICAO_SIMULTANEA, MSG_EDICAO_SIMULTANEA);
        } catch (DataIntegrityViolationException ex) {
            // Duas pessoas cadastrando o mesmo documento ao mesmo tempo: o índice único segura a segunda
            throw new ConflictException(CODIGO_DUPLICADO, MSG_DUPLICADO);
        }
    }

    private ClienteResponse toResponse(Cliente c) {
        AcessoAppResponse acesso = c.getId() == null
                ? AcessoAppResponse.semAcesso()
                : tokenRepository.findByClienteIdAndRevogadoEmIsNull(c.getId())
                        .map(t -> new AcessoAppResponse(true, t.getCriadoEm(), t.getUltimoUsoEm()))
                        .orElseGet(AcessoAppResponse::semAcesso);
        return new ClienteResponse(c.getId(), c.getTipoPessoa(), c.getDocumento(), c.getNome(), c.getNomeFantasia(),
                c.getEmail(), c.getTelefone(), c.getRegimeTributario(), c.getObservacoes(), c.getEndereco(), c.isAtivo(),
                c.getDataInclusao(), c.getDataAtualizacao(), c.getVersao(), acesso);
    }

    /**
     * Valida e limpa os dados: documento e telefone sem pontuação, e-mail em minúsculas, textos sem espaços nas
     * pontas e vazios como nulos. Nome fantasia e regime tributário só valem para pessoa jurídica.
     */
    static DadosCliente normalizar(ClienteRequest request) {
        TipoPessoa tipo = request.tipoPessoa();
        String documento = DocumentoFiscal.normalizar(request.documento(), tipo);

        String nome = texto(request.nome());
        if (nome == null) {
            throw new BusinessException("Informe o nome.");
        }

        String telefone = texto(request.telefone());
        if (telefone != null) {
            telefone = telefone.replaceAll("\\D", "");
            if (telefone.length() < 10 || telefone.length() > 11) {
                throw new BusinessException("Telefone inválido. Informe o DDD e o número.");
            }
        }

        String email = texto(request.email());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }

        boolean juridica = tipo == TipoPessoa.JURIDICA;
        return new DadosCliente(tipo, documento, nome,
                juridica ? texto(request.nomeFantasia()) : null,
                email, telefone,
                juridica ? request.regimeTributario() : null,
                texto(request.observacoes()),
                normalizarEndereco(request.endereco()));
    }

    static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    /** Endereço opcional: CEP só com dígitos (8), UF em maiúsculas e da lista, textos vazios como nulos. */
    static EnderecoDto normalizarEndereco(EnderecoDto endereco) {
        if (endereco == null) {
            return new EnderecoDto(null, null, null, null, null, null, null);
        }
        String cep = texto(endereco.cep());
        if (cep != null) {
            cep = cep.replaceAll("\\D", "");
            if (cep.length() != 8) {
                throw new BusinessException("CEP inválido. Informe os 8 números.");
            }
        }
        String uf = texto(endereco.uf());
        if (uf != null) {
            uf = uf.toUpperCase(Locale.ROOT);
            if (!UFS.contains(uf)) {
                throw new BusinessException("UF inválida.");
            }
        }
        return new EnderecoDto(cep, texto(endereco.logradouro()), texto(endereco.numero()),
                texto(endereco.complemento()), texto(endereco.bairro()), texto(endereco.cidade()), uf);
    }

    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.strip();
        return limpo.isEmpty() ? null : limpo;
    }

    /** Tenant obrigatório + status + busca por nome, nome fantasia, e-mail (sem diferenciar acentos) ou documento. */
    static Specification<Cliente> filtro(Long tenantId, String busca, StatusFiltro status) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("tenantId"), tenantId));

            if (status == StatusFiltro.ATIVOS) {
                predicados.add(cb.isTrue(root.get("ativo")));
            } else if (status == StatusFiltro.INATIVOS) {
                predicados.add(cb.isFalse(root.get("ativo")));
            }

            String termo = texto(busca);
            if (termo != null) {
                // Nome, nome fantasia e e-mail ficam na coluna "busca", já sem acentos e em minúsculas
                String padrao = "%" + escaparLike(TextoBusca.normalizar(termo)) + "%";
                List<Predicate> ou = new ArrayList<>();
                ou.add(cb.like(root.get("busca"), padrao, '\\'));

                String documento = termo.replaceAll("[\\s./-]", "").toUpperCase(Locale.ROOT);
                if (!documento.isEmpty() && documento.chars().anyMatch(Character::isDigit)) {
                    ou.add(cb.like(root.get("documento"), "%" + escaparLike(documento) + "%", '\\'));
                }
                predicados.add(cb.or(ou.toArray(Predicate[]::new)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
