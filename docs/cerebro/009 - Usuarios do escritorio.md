---
projeto: manager_accounting
registro: 009
titulo: Usuários do escritório (perfis e convite por e-mail)
data: 2026-10-21
status: em revisão (branch feature/usuarios-escritorio, ainda não validado no Maven)
modulo: usuarios
tags:
  - manager_accounting
  - registro
  - usuarios
  - permissoes
  - seguranca
  - backend
  - frontend
---

# 009 - Usuários do escritório

Voltar para [[manager_accounting - Índice]] · Anterior: [[008 - Meu perfil]] · Próximo: [[010 - Ajustes nos clientes]]

## Resumo

O administrador do escritório convida, muda o perfil, desativa e reativa os usuários do próprio escritório, na nova tela **Usuários** (`/app/usuarios`), que só aparece para administradores.

- **Branch:** `feature/usuarios-escritorio`, criada a partir da `feature/meu-perfil` (que ainda não está na `main`). Faça o merge depois do Meu perfil.
- **Pull request:** https://github.com/MarcosVenicioSJr/manager_accounting/pull/new/feature/usuarios-escritorio

## Perfis

| Perfil | Pode |
|---|---|
| **Administrador** (`ADMIN`) | Tudo: gerenciar usuários, cadastrar/editar clientes, gerar token do app, **desativar/reativar clientes** e **revogar o acesso ao app** |
| **Usuário** (`USUARIO`) | Consultar, cadastrar e editar clientes e gerar (ou gerar de novo) o token do app |

Definido por você: o usuário comum cadastra, edita e gera token (por enquanto).

A migration torna **administradores todos os usuários que já existem** (até aqui cada escritório tinha só o contador cadastrado pelo banco). O primeiro usuário de cada escritório novo continua sendo cadastrado pelo banco, agora com `perfil = 'ADMIN'` (ver `docs/sql/seed-dev.sql`).

O perfil é lido do banco a cada requisição: mudar o perfil ou desativar vale na hora.

## Convite (definido por você: link por e-mail)

1. O administrador informa **nome, e-mail, usuário de acesso e perfil**. O login é sugerido a partir do nome ("Fernanda de Oliveira Lima" → `fernanda.lima`) e pode ser trocado.
2. O usuário é criado na hora como **pendente**: login reservado, senha aleatória que ninguém conhece.
3. A pessoa recebe "Convite para o backoffice" com o link `<WEB_BASE_URL>/aceitar-convite#token=...` (vale **48 horas**, uso único).
4. Pelo link ela confere o nome, informa o **telefone** e **cria a senha**. Isso conclui o primeiro acesso; depois ela entra pelo login normal.

Na lista, o pendente aparece como "Convite enviado (vale até ...)" ou "Convite expirado".

## Ações na lista (menu ⋯ de cada linha)

| Ação | Regras |
|---|---|
| Reenviar convite | Só pendente e ativo. Novo link; o anterior para de valer. Mínimo de 1 minuto entre envios |
| Cancelar convite | Só pendente. **Apaga o usuário** e libera o login (para corrigir e-mail ou login digitado errado) |
| Tornar administrador / usuário comum | Não vale para si mesmo |
| Desativar acesso | Não vale para si mesmo. Encerra as sessões na hora e anula convites em aberto |
| Reativar acesso | Volta com a mesma senha; quem ainda não tinha criado a senha precisa de um novo convite |

**O escritório nunca fica sem administrador em atividade** (ativo e com a senha criada): rebaixar ou desativar o último responde "O escritório precisa de pelo menos um administrador ativo...".

> [!note] Escolhas minhas, não definidas por você
> Validade de 48 horas, sugestão de login pelo nome, formato do login (3 a 60 letras minúsculas, números, ponto, hífen, sublinhado), "Cancelar convite" apagar o usuário pendente, ninguém mudar o próprio perfil nem se desativar, e a trava do último administrador.

## Banco (Flyway)

| Migration | Conteúdo |
|---|---|
| `V7__usuarios_escritorio.sql` | `usuario.perfil` (`ADMIN`/`USUARIO`, padrão `USUARIO`; existentes viram `ADMIN`); `convite_usuario` (usuario_id com `ON DELETE CASCADE`, token_hash único, criado_por, expira_em, aceito_em, invalidado_em) |

## Endpoints

| Método e rota | Acesso | Resposta |
|---|---|---|
| `GET /api/usuarios` | Admin | Usuários do escritório: login, nome, e-mail, perfil, situação (`ATIVO`/`PENDENTE`/`INATIVO`), validade do convite, `voce` |
| `POST /api/usuarios` | Admin | `{ nomeCompleto, email, login, perfil }` → 201; 409 `LOGIN_EM_USO` |
| `POST /api/usuarios/{id}/convite` | Admin | Reenvia o convite |
| `DELETE /api/usuarios/{id}/convite` | Admin | Cancela o convite (apaga o pendente) |
| `PUT /api/usuarios/{id}/perfil` | Admin | `{ perfil }` |
| `POST /api/usuarios/{id}/desativar` · `/reativar` | Admin | 204 |
| `POST /api/auth/convite/validar` | Público | `{ nomeCompleto, login, email, nomeEscritorio }`; 400 convite inválido |
| `POST /api/auth/convite/aceitar` | Público | `{ token, nomeCompleto, telefone, novaSenha }` → `{ login }` |
| `GET /api/usuario/me` | Contador | Agora devolve também `perfil` |

Passaram a exigir **administrador**: `POST /api/clientes/{id}/desativar`, `POST /api/clientes/{id}/reativar` e `DELETE /api/clientes/{id}/acesso-app`.

## Backend

| Arquivo | O que faz |
|---|---|
| `modules/equipe/EquipeService` | Lista, convite, reenviar, cancelar, perfil, desativar/reativar, aceite; trava do último administrador |
| `modules/equipe/EquipeController`, `ConviteController` | Endpoints acima |
| `modules/equipe/ConviteUsuario` + repositório, `EquipeEmails`, `EquipeProperties`, `dto/*` | Convite, e-mail e configuração |
| `modules/usuario/PerfilAcesso`, `Usuario`, `UsuarioRepository` | Perfil, usuário convidado, consultas por escritório e contagem de administradores |
| `modules/usuario/Telefones` | Normalização de telefone (agora usada pelo Meu perfil e pelo convite) |
| `core/security/JwtAuthenticationFilter` | Administrador recebe `ROLE_ADMIN` além de `ROLE_CONTADOR` |
| `core/security/SecurityConfig` | Rotas de administrador e rotas públicas do convite |
| `modules/auth/RefreshTokenService` | `apagarTodos` (antes de apagar um pendente) |

Testes novos: `EquipeServiceTest` (14 casos).

## Front

| Arquivo | O que faz |
|---|---|
| `pages/usuarios/UsuariosPage` + `usuarios.css` | Lista, menu de ações, diálogo "Convidar usuário" e confirmações. Usuário comum que abrir a rota vê "Acesso só para administradores" |
| `pages/aceitar-convite/AceitarConvitePage` | Tela do link (visual das telas de acesso): nome, telefone, nova senha |
| `features/equipe/equipeService` | Chamadas à API |
| `components/ui/RowMenu` | Menu ⋯ acessível (teclado, Esc, setas) |
| `layouts/BackofficeLayout` | Item "Usuários" no menu, só para administrador |
| `pages/clientes/ClienteDetalhePage`, `features/clientes/AcessoAppCard` | "Desativar/Reativar cliente" e "Revogar acesso" só para administrador |

## Configuração

| Variável | Padrão |
|---|---|
| `EQUIPE_CONVITE_VALIDADE` | `48h` |
| `EQUIPE_CONVITE_INTERVALO_MINIMO` | `1m` |

## Como testar

1. Suba tudo e entre como administrador. Menu → **Usuários** → "Convidar usuário".
2. Abra o Mailpit, clique em "Criar minha senha", preencha e entre com o novo usuário (perfil Usuário).
3. Como usuário comum: o menu não tem "Usuários"; no cliente não aparecem "Desativar cliente" nem "Revogar acesso"; `POST /api/clientes/{id}/desativar` responde 403.
4. Como administrador: torne o outro usuário administrador e volte; tente se rebaixar ou se desativar (não deixa); tente rebaixar o único administrador (não deixa).
5. Desative o usuário comum com ele logado em outro navegador: a próxima ação dele cai no login.
6. Convide alguém e cancele: o login fica livre para um novo convite.

## Validação

- [x] Migration `V7` aplicada sobre um banco já existente (o usuário virou `ADMIN`) e num banco limpo com o seed novo; check do perfil e `ON DELETE CASCADE` do convite testados
- [x] Código Java novo compilado com `javac` contra as assinaturas das bibliotecas
- [x] **Fluxo do `EquipeService` executado** com repositórios em memória: 34 verificações (convite, login duplicado e de outro escritório, reenvio, aceite, uso único, isolamento, próprio perfil, último administrador, desativar/reativar, cancelar, expiração). O fluxo do Meu perfil (32 verificações) continua passando depois da mudança de telefone
- [x] Front: `npm run build` e `oxlint` sem erros nem avisos
- [x] Telas no Chromium (API simulada): lista, menu de ações, convite com login sugerido e login em uso, desativar, visão do usuário comum, aceite do convite, celular 390 px
- [ ] **`mvn test`** e backend real (Maven bloqueado no ambiente de criação)

## Pontos de atenção

- Dois administradores rebaixando um ao outro **exatamente ao mesmo tempo** poderiam, em tese, deixar o escritório sem administrador (a conferência não trava a linha). Improvável com poucos usuários; se acontecer, corrige-se pelo banco.
- Usuários cadastrados pelo banco sem `perfil` ficam como `USUARIO` (padrão da coluna). O primeiro de cada escritório precisa de `'ADMIN'`.
