---
projeto: manager_accounting
registro: 015
titulo: Revisão geral do sistema e correções
data: 2026-10-29
status: em revisão (PR aberto; falta rodar mvn test localmente)
modulo: geral
tags:
  - manager_accounting
  - registro
  - seguranca
  - backend
  - frontend
---

# 015 - Revisão geral e correções

Voltar para [[manager_accounting - Índice]] · Anterior: [[014 - Relatorio por categoria]]

## Resumo

Antes da revisão, todas as branches pendentes foram conferidas: as 12 já estavam dentro da `main`, então não houve merge a fazer.

Depois veio uma revisão do sistema inteiro: banco, SQL, back-end e front. Foram corrigidos três problemas e um detalhe de código.

- **Branch:** `fix/revisao-geral`.

## O que foi testado

| Parte | Como | Resultado |
|---|---|---|
| Banco | As migrations V1 a V10 e os scripts `seed-dev.sql` e `tenant-illum.sql` rodados num PostgreSQL 16 limpo | Tudo roda sem erro |
| Seed | Hash da senha temporária conferido com BCrypt | `Temp@1234` confere |
| SQL do dashboard e dos relatórios | Consultas executadas com os parâmetros tipados como o driver JDBC envia, com 60 lançamentos de exemplo | Resultados corretos |
| Contratos da API | Campos de todos os DTOs do back comparados com os tipos do front | Batem |
| Funções do front | Máscara de valor, conversão para a API, datas, períodos, CPF/CNPJ (inclusive alfanumérico) | Corretas |
| Front | `npm run build` e `npm run lint` | Sem erros |
| Back | Revisão de código de todos os módulos; arquivos alterados checados com `javac` | Sem erro de sintaxe ou de tipo |

O `mvn test` não rodou: o ambiente da revisão não tinha acesso ao repositório do Maven.

## Correções

### 1. Troca de senha não derrubava o access token (segurança)

**Problema:** ao redefinir a senha ("Esqueceu sua senha?") ou confirmar a troca no Meu perfil, o sistema revogava os refresh tokens e dizia que todas as sessões tinham sido encerradas. Só que o access token (JWT) de quem já estava logado continuava valendo até expirar, por até 4 horas. Quem tivesse roubado a sessão continuava dentro.

**Correção:**

- Nova coluna `usuario.sessoes_validas_desde` (migration `V10`).
- `Usuario.encerrarSessoes(agora)` grava o instante, truncado em segundos (a precisão do `iat` do JWT).
- `JwtService` passa a devolver o `iat` do token (`TokenClaims.emitidoEm`), e token sem `iat` é recusado.
- `JwtAuthenticationFilter` recusa o token emitido antes desse instante: a requisição vira 401, o front tenta o refresh (que já foi revogado) e volta para o login.
- Chamado em `RecuperacaoSenhaService.redefinir` e em `PerfilService.confirmar` (troca de senha).

Um login feito no mesmo segundo da troca continua valendo.

### 2. Erro 500 chegava ao front como "Não autenticado"

**Problema:** quando acontecia um erro inesperado no servidor (ou alguém chamava uma rota que não existe), o Spring redirecionava internamente para `/error`. Essa rota exigia login, e o redirecionamento não leva o login junto. A resposta virava 401. O front, ao ver o 401, renovava a sessão e reenviava a mesma requisição, e no fim mostrava "Não autenticado" no lugar do erro real.

**Correção:** `/error` liberado no `SecurityConfig`. Os erros voltam com o status certo (500, 404).

### 3. Lançamentos: link com cliente inexistente

**Problema:** na página de lançamentos aberta com `?cliente=<id>` de um cliente que não existe (ou de outro escritório), o filtro mostrava "Cliente não encontrado", mas o botão "Novo lançamento" abria o formulário já preenchido com esse cliente. Ao salvar, dava erro.

**Correção:** o formulário só vem preenchido quando o cliente do filtro foi encontrado.

### 4. Detalhe

Import duplicado de `java.util.Set` no `ClienteService`.

## Arquivos

| Arquivo | O que mudou |
|---|---|
| `db/migration/V10__usuario_sessoes_validas_desde.sql` | Coluna nova |
| `modules/usuario/Usuario.java` | `encerrarSessoes`, `tokenEncerrado`, getter |
| `core/security/JwtService.java` | `TokenClaims` com `emitidoEm` |
| `core/security/JwtAuthenticationFilter.java` | Recusa token anterior à troca de senha |
| `core/security/SecurityConfig.java` | `/error` liberado |
| `modules/auth/RecuperacaoSenhaService.java` | Encerra as sessões ao redefinir |
| `modules/perfil/PerfilService.java` | Encerra as sessões ao confirmar a troca |
| `modules/cliente/ClienteService.java` | Import duplicado |
| `web/src/features/lancamentos/LancamentosPainel.tsx` | Cliente inexistente não preenche o formulário |

## Testes

- **Novo:** `JwtAuthenticationFilterTest`, com três casos: token válido, token anterior à troca de senha e login no mesmo segundo da troca.
- **Ajustados:** `JwtServiceTest` (o `iat` vem nas claims), `RecuperacaoSenhaServiceTest` e `PerfilServiceTest` (as sessões são encerradas na troca de senha, e a troca de e-mail não encerra).

## Observações

- `npm audit` aponta uma vulnerabilidade moderada no `uuid`, que vem do `exceljs`. Ela só afeta funções de UUID que o sistema não usa. A correção sugerida rebaixa o `exceljs` para a 3.4, por isso ficou como está.
- O build do front avisa que os arquivos do PDF e do Excel passam de 500 KB. Eles já carregam só quando alguém gera um relatório, então não pesam na abertura do sistema.

## Falta validar (Marcos)

```bash
cd backend
mvn test
mvn spring-boot:run   # o Flyway aplica a V10
```

Depois, um teste manual: entre em dois navegadores, troque a senha no Meu perfil por um deles e confirme pelo e-mail no Mailpit. O outro navegador deve voltar para o login na próxima ação.
