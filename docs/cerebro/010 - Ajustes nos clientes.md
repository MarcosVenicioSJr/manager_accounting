---
projeto: manager_accounting
registro: 010
titulo: Ajustes nos clientes (endereço com busca por CEP e busca sem acento)
data: 2026-10-21
status: em revisão (branch feature/ajustes-clientes, ainda não validado no Maven)
modulo: clientes
tags:
  - manager_accounting
  - registro
  - clientes
  - endereco
  - busca
  - backend
  - frontend
---

# 010 - Ajustes nos clientes

Voltar para [[manager_accounting - Índice]] · Anterior: [[009 - Usuarios do escritorio]]

## Resumo

- **Endereço** no cadastro do cliente, com **busca por CEP** (definido por você: completo, com busca por CEP, tudo opcional).
- **Busca sem diferenciar acentos:** "joao" encontra "João", "conceicao" encontra "Conceição".
- **Exclusão de cliente:** não existe, continua só desativar (definido por você).
- Na lista, a linha do cliente mostra a cidade/UF quando há endereço.

- **Branch:** `feature/ajustes-clientes`, criada a partir da `feature/usuarios-escritorio`. Faça o merge depois dela.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/ajustes-clientes

## Endereço

| Campo | Regra |
|---|---|
| CEP | Opcional; máscara `00000-000`; guardado com 8 números |
| Logradouro, número, complemento, bairro, cidade | Opcionais (150, 20, 100, 100 e 100 caracteres) |
| UF | Opcional; lista das 27 UFs |

**Busca por CEP:** ao completar os 8 números, o front consulta o **ViaCEP** (serviço público e gratuito) e preenche logradouro, bairro, cidade e UF; o cursor vai para o **número**. Se o CEP não existir ou o ViaCEP estiver fora do ar, aparece um aviso e a pessoa preenche à mão. Dá para editar tudo depois.

A consulta sai **do navegador direto para o ViaCEP**, sem o cliente da nossa API: nenhum token ou cookie do backoffice vai para o site externo (conferido no teste).

## Busca sem acento

O cliente ganhou a coluna `busca`: nome, nome fantasia e e-mail em minúsculas e sem acentos. O backend preenche a cada gravação, e a busca compara o termo tratado do mesmo jeito. CPF/CNPJ continuam buscados pelo número (com ou sem pontuação).

Escolha minha: em vez da extensão `unaccent` do PostgreSQL (que exige permissão de superusuário para instalar), a migration preenche os clientes antigos com `translate()` e o Java faz o resto. O resultado é o mesmo (conferido no banco: `JOÃO CONCEIÇÃO Müller` → `joao conceicao muller`).

## Banco (Flyway)

| Migration | Conteúdo |
|---|---|
| `V8__cliente_endereco_busca.sql` | Colunas `cep`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf` (checks de CEP e UF) e `busca` (preenchida para os clientes que já existem) |

## API

| Mudança | Detalhe |
|---|---|
| `POST/PUT /api/clientes` | Novo campo opcional `endereco: { cep, logradouro, numero, complemento, bairro, cidade, uf }` |
| `GET /api/clientes/{id}` | Devolve `endereco` |
| `GET /api/clientes` | Cada item traz `cidade` e `uf`; `busca` ignora acentos |

## Arquivos

| Arquivo | O que mudou |
|---|---|
| `modules/cliente/Cliente`, `DadosCliente`, `ClienteService` | Endereço (normalizado: CEP só números, UF em maiúsculas e da lista), coluna `busca` e filtro usando ela |
| `modules/cliente/TextoBusca` | Minúsculas e sem acentos |
| `modules/cliente/dto/EnderecoDto` + `ClienteRequest`, `ClienteResponse`, `ClienteListaItem` | Endereço na API |
| `features/clientes/cep.ts` | Máscara, lista de UFs e consulta ao ViaCEP |
| `features/clientes/ClienteForm` + `clientes.css` | Seção Endereço com o estado da busca (buscando, preenchido, não encontrado, falha) |
| `features/clientes/types`, `clienteFormValores`, `pages/clientes/ClientesPage` | Tipos e cidade/UF na lista |

Testes: `ClienteServiceTest` com 3 casos novos (endereço normalizado, CEP/UF inválidos, texto de busca sem acentos); as chamadas existentes passaram a informar o endereço.

## Como testar

1. Novo cliente → digite um CEP real (ex.: 60115-170): logradouro, bairro, cidade e UF são preenchidos e o cursor vai para o número.
2. Digite 99999-999: "CEP não encontrado". Digite só 4 números e salve: "CEP incompleto".
3. Salve, volte à lista: a linha mostra "Fortaleza/CE".
4. Cadastre "João Conceição" e busque por "joao conceicao".

## Validação

- [x] Migration `V8` sobre banco existente: clientes antigos com a coluna `busca` preenchida corretamente, inclusive maiúsculas acentuadas e com locale `C`; checks de CEP e UF
- [x] Código Java compilado com `javac` contra as assinaturas das bibliotecas; normalização e endereço executados (10 verificações)
- [x] Front: `npm run build` e `oxlint` sem erros nem avisos
- [x] Telas no Chromium (API e ViaCEP simulados): busca sem acento, CEP buscando/preenchido/não encontrado/falha, CEP incompleto, edição, celular 390 px
- [ ] **`mvn test`** e backend real (Maven bloqueado no ambiente de criação)
- [ ] Busca por CEP real (o ViaCEP é bloqueado no ambiente de criação; foi simulado)
