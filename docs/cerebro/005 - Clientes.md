---
projeto: manager_accounting
registro: 005
titulo: Clientes (cadastro e token do app)
data: 2026-10-19
status: em revisão (branch feature/area-logada-clientes, ainda não validado no Maven)
modulo: clientes
commits:
  - Área logada do backoffice com o tema do escritório e módulo de clientes
tags:
  - manager_accounting
  - registro
  - clientes
  - multi-tenant
  - backend
  - frontend
---

# 005 - Clientes

Voltar para [[manager_accounting - Índice]] · Anterior: [[004 - Area logada e tema do escritorio]]

## Resumo

O contador cadastra, lista, busca, edita, desativa e reativa os clientes do escritório, sempre isolados pelo tenant do token. Na tela do cliente ele gera o **token de acesso ao app**, que aparece uma única vez para ser enviado ao cliente; dá para gerar de novo ou revogar.

## Telas

| Rota | O que tem |
|---|---|
| `/app/clientes` | Busca (nome, nome fantasia, e-mail ou CPF/CNPJ, com ou sem pontuação), filtro Ativos / Inativos / Todos, tabela com 20 por página e coluna "App" (Liberado / Sem acesso). Busca, filtro e página ficam na URL. No celular cada linha vira um cartão |
| `/app/clientes/novo` | Formulário de cadastro. Ao salvar, abre o detalhe do cliente |
| `/app/clientes/:id` | Formulário de edição + cartão "Acesso ao app" + cartão "Registro" (cadastro e última alteração) + botão Desativar/Reativar |

Estados tratados: carregando (skeleton), erro com "Tentar novamente", nenhum cliente cadastrado, busca sem resultado (com "Limpar busca" e "Buscar em todos"), filtro vazio, cliente não encontrado (inclusive de outro escritório).

## Campos do cliente

| Campo | Regra |
|---|---|
| Tipo de pessoa | Pessoa jurídica (padrão) ou física |
| CPF / CNPJ | Obrigatório, com dígito verificador. **O CNPJ aceita o formato alfanumérico da Receita (em vigor desde julho de 2026)**, além do numérico. Guardado sem pontuação e em maiúsculas. Único por escritório (o mesmo CPF/CNPJ pode existir em escritórios diferentes) |
| Razão social / Nome completo | Obrigatório, até 150 caracteres |
| Nome fantasia | Só pessoa jurídica, até 150 |
| Regime tributário | Só pessoa jurídica, opcional: MEI, Simples Nacional, Lucro Presumido, Lucro Real |
| E-mail | Opcional, guardado em minúsculas |
| Telefone | Opcional, com DDD (10 ou 11 dígitos), guardado só com números |
| Observações | Opcional, até 1000 caracteres (anotações internas do escritório) |

Ao trocar o tipo de pessoa, o documento é limpo. Para pessoa física, nome fantasia e regime são descartados.

> [!note] Escolhas minhas, não definidas por você
> Os campos acima (principalmente regime tributário e observações), o CNPJ como padrão, desativar revogar o token, reativar não devolver o token, 20 por página, o formato do token e a ordenação por nome. É só pedir para mudar. Endereço não entrou.

## Token de acesso ao app

| Tema | Decisão |
|---|---|
| Formato | 16 caracteres em 4 grupos: `XXXX-XXXX-XXXX-XXXX`, alfabeto Base32 de Crockford (sem I, L, O e U, que confundem). 80 bits aleatórios |
| Digitação | O hash é feito sobre o valor normalizado: maiúsculas, sem hífens/espaços, e O→0, I/L→1. O cliente pode digitar de qualquer jeito |
| Armazenamento | Só o hash SHA-256 (`cliente_token_acesso.token_hash`). O valor aparece **uma vez** na tela, com botão de copiar |
| Quantidade | No máximo um token ativo por cliente (índice único parcial). Gerar outro revoga o anterior na hora |
| Concorrência | Gerar trava a linha do cliente (`PESSIMISTIC_WRITE`): dois cliques ao mesmo tempo não deixam dois tokens ativos |
| Cliente inativo | Não gera token. **Desativar revoga o token**; reativar não devolve (gera-se um novo) |
| Auditoria | Guarda quem gerou (`criado_por`) e quando; geração e revogação vão para o log com tenant, cliente e usuário (nunca o token) |
| Uso no app | `ultimo_uso_em` já existe e será preenchido na etapa do app (React Native). Validade/expiração do token: a definir nessa etapa |

## Isolamento por tenant

- O tenant vem **sempre** do token do contador (`AuthenticatedUser.tenantId`), nunca da URL ou do corpo.
- Toda consulta do repositório recebe o tenant (`findByIdAndTenantId`, filtro da lista); não existe busca de cliente só pelo id.
- Cliente de outro escritório responde **404 "Cliente não encontrado"**, igual a um id que não existe, para não revelar que ele existe.

## Edição simultânea

O cliente tem `versao` (optimistic lock). A tela manda a versão que carregou; se outra pessoa salvou antes, a API responde 409 `EDICAO_SIMULTANEA` e a tela mostra o aviso com "Recarregar cliente". Desativar/reativar também aumenta a versão.

## Banco (Flyway)

| Migration | Conteúdo |
|---|---|
| `V5__cliente.sql` | `cliente` (tenant_id, tipo_pessoa, documento, nome, nome_fantasia, email, telefone, regime_tributario, observacoes, ativo, data_inclusao, data_atualizacao, versao), com checks de formato do documento por tipo, telefone, regime e regime só para PJ; único `(tenant_id, documento)`; índice `(tenant_id, lower(nome))` para a lista. `cliente_token_acesso` (cliente_id, token_hash único, criado_em, criado_por, revogado_em, ultimo_uso_em) com índice único parcial de token ativo |

O `docs/sql/seed-dev.sql` agora também cadastra 5 clientes de exemplo no Escritório Exemplo (rodar de novo não duplica).

## Endpoints (contador logado, `ROLE_CONTADOR`)

| Método e rota | Corpo / parâmetros | Resposta |
|---|---|---|
| `GET /api/clientes` | `busca`, `status` (`ATIVOS` padrão, `INATIVOS`, `TODOS`), `pagina` (0), `tamanho` (20, máx. 100) | `{ itens, pagina, tamanho, total, totalPaginas }` |
| `GET /api/clientes/resumo` | | `{ ativos, inativos, comAcessoApp }` |
| `GET /api/clientes/{id}` | | Cliente + `acessoApp { ativo, geradoEm, ultimoUsoEm }` |
| `POST /api/clientes` | Dados do cliente | 201 + `Location`; 400 validação; 409 `DOCUMENTO_DUPLICADO` |
| `PUT /api/clientes/{id}` | Dados + `versao` | 200; 400; 404; 409 `DOCUMENTO_DUPLICADO` ou `EDICAO_SIMULTANEA` |
| `POST /api/clientes/{id}/desativar` | | 204 (repetir não muda nada) |
| `POST /api/clientes/{id}/reativar` | | 204 |
| `POST /api/clientes/{id}/acesso-app` | | 201 `{ token, geradoEm }` (única vez que o token aparece); 400 se inativo |
| `DELETE /api/clientes/{id}/acesso-app` | | 204 |

Erros novos no `GlobalExceptionHandler`: 404 (`NotFoundException`), 409 com `codigo` (`ConflictException`), JSON malformado e parâmetro com tipo errado viram 400 "Dados inválidos" / "Parâmetro inválido".

## Backend

| Arquivo | O que faz |
|---|---|
| `modules/cliente/ClienteService` | Regras: normalização, duplicidade, versão, desativar/reativar, gerar/revogar token, lista com filtro e resumo |
| `modules/cliente/ClienteController` | Endpoints acima |
| `modules/cliente/Cliente`, `ClienteRepository` | Entidade e consultas sempre com tenant (inclui a busca com trava) |
| `modules/cliente/ClienteTokenAcesso`, `ClienteTokenAcessoRepository` | Token do app (hash) e consultas |
| `modules/cliente/DocumentoFiscal` | Validação de CPF e CNPJ (numérico e alfanumérico) |
| `modules/cliente/TokenAcessoApp` | Geração, normalização e hash do token |
| `modules/cliente/dto/*` | Requisição e respostas |
| `core/exception/NotFoundException`, `ConflictException` | 404 e 409 |

Testes novos: `ClienteServiceTest` (21 casos: tenant do token, normalização, PF sem fantasia/regime, duplicado e duplicado simultâneo, telefone/nome/documento inválidos, **todas as operações com tenant errado respondem 404 sem gravar nada**, versão ausente/antiga/colisão no banco, documento de outro cliente, desativar revoga token, desativar idempotente, reativar, token só com hash, token para inativo, revogar, lista com limite de tamanho e coluna App, lista vazia), `DocumentoFiscalTest` (CPF/CNPJ válidos e inválidos, exemplo oficial do CNPJ alfanumérico `12.ABC.345/01DE-35`) e `TokenAcessoAppTest` (formato, sem repetição, normalização).

## Front

| Arquivo | O que faz |
|---|---|
| `pages/clientes/ClientesPage` | Lista com busca (espera 300 ms), filtro, paginação e estados vazios. Cancela a requisição anterior a cada mudança |
| `pages/clientes/NovoClientePage` | Cadastro |
| `pages/clientes/ClienteDetalhePage` | Edição, situação e cartões laterais |
| `features/clientes/ClienteForm` | Formulário com validação no navegador (mesmas regras do backend), erro por campo ligado por `aria-describedby`, foco no primeiro erro, aviso de "Sair sem salvar?" ao trocar de página e ao fechar a aba |
| `features/clientes/AcessoAppCard` | Gerar, gerar de novo (com confirmação), revogar (com confirmação) e o diálogo do token com "Copiar" (não fecha clicando fora) |
| `features/clientes/documento`, `formatar` | Validação e máscaras de CPF/CNPJ e telefone, datas em pt-BR |
| `features/clientes/clienteService`, `types`, `clienteFormValores` | Chamadas à API e tipos |
| `features/clientes/clientes.css` | Estilos de lista, formulário e detalhe |
| `services/errors` | `getErrorCode` e `getErrorStatus` |

## Plano de testes (para a revisão)

| ID | Cenário | Resultado esperado |
|---|---|---|
| CT01 | Cadastrar PJ com CNPJ numérico válido | Salva, abre o detalhe, aviso "Cliente cadastrado" |
| CT02 | Cadastrar PJ com CNPJ alfanumérico `12.ABC.345/01DE-35` | Salva; aparece formatado na lista |
| CT03 | Cadastrar PF com CPF válido, sem e-mail e telefone | Salva; lista mostra "Sem contato" |
| CT04 | CPF/CNPJ com dígito errado, nome vazio, e-mail `a@`, telefone sem DDD | Erro em cada campo, foco no primeiro, nada enviado |
| CT05 | CPF/CNPJ já cadastrado no mesmo escritório | Erro no campo documento (409) |
| CT06 | Mesmo CPF/CNPJ em outro escritório | Salva normalmente |
| CT07 | Buscar por parte do nome, por e-mail, por CNPJ com e sem pontuação | Encontra; URL guarda a busca |
| CT08 | Filtro Inativos / Todos; paginação com mais de 20 | Lista certa; voltar do detalhe mantém filtro e página |
| CT09 | Editar e salvar | "Alterações salvas"; "Última alteração" atualiza |
| CT10 | Editar em duas abas e salvar nas duas | A segunda recebe o aviso de edição simultânea; "Recarregar cliente" traz a versão nova |
| CT11 | Editar e clicar em "Clientes" sem salvar | Pergunta "Sair sem salvar?" |
| CT12 | Gerar token | Diálogo com o token e "Copiar"; ao fechar, só aparece "Acesso liberado" e a data |
| CT13 | Gerar novo token | Confirmação; o anterior para de valer (conferir no banco: um único `revogado_em` nulo) |
| CT14 | Revogar acesso | Confirmação; cartão volta para "Gerar token de acesso"; lista mostra "Sem acesso" |
| CT15 | Desativar cliente com token | Confirmação; cliente sai dos ativos e o token é revogado; não deixa gerar token |
| CT16 | Reativar | Volta para ativos, sem token |
| CT17 | **Segurança:** com o login do escritório A, chamar `GET/PUT/POST .../clientes/{id do escritório B}` | 404 em todas, nada alterado |
| CT18 | **Segurança:** enviar `tenantId` no corpo do POST | Ignorado; o cliente fica no escritório do token |
| CT19 | `/app/clientes/abc` e `/app/clientes/999999` | "Cliente não encontrado" |
| CT20 | Celular (390 px): lista, detalhe, gaveta do menu | Sem rolagem horizontal; tudo utilizável com o polegar |

## Validação

- [x] Migration `V5` executada em PostgreSQL 16 junto com as anteriores e o seed; checks e índices únicos testados com inserts válidos e inválidos; a lista usa o índice `(tenant_id, lower(nome))`
- [x] Validador de CPF/CNPJ executado (Java e TypeScript) com o exemplo oficial do CNPJ alfanumérico
- [x] Código Java novo compilado com `javac` contra assinaturas das bibliotecas (Spring, JPA, validação)
- [x] Front: `npm run build` e `oxlint` sem erros nem avisos
- [x] Telas no Chromium com API simulada: lista, busca, busca vazia, filtros, paginação, detalhe, salvar, sair sem salvar, gerar/regerar token, desativar, erros do formulário, duplicado, edição simultânea, não encontrado, carteira vazia; desktop, tablet e celular
- [ ] **`mvn test`** e subir o backend de verdade (Maven bloqueado no ambiente de criação)
- [ ] Roteiro CT01–CT20 com backend real

## Pontos de atenção

- Se o backend não subir, o mais provável é detalhe de mapeamento JPA ou da `Specification`, já que não houve build real. Mande o log.
- A busca diferencia acentos ("Joao" não acha "João"). Dá para resolver com a extensão `unaccent` do PostgreSQL depois.
- Não existe exclusão de cliente, só desativação (os lançamentos do app vão depender dele).

## Próximo passo

[[manager_accounting - Índice]] → etapa 3: app do cliente (React Native) entrando com o token, com lançamento de compras, dívidas e despesas.
