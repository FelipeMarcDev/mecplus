# API do Mec+

Base: `http://localhost:8080/api` — todas as respostas são JSON.


```js
fetch("http://localhost:8080/api/clientes", { credentials: "include" })
```

Use `localhost` (e não `127.0.0.1`) tanto no front quanto na API, senão o navegador não envia o cookie.
Origens liberadas por padrão: `localhost` nas portas 5173, 3000, 4200, 5500 e 8081 (altere com
`mecplus.cors.origens=...` no `application.properties`).

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| POST | `/auth/login` | `{ "login", "senha" }` | `{ id, nome, perfil, permissoes[] }` |
| GET | `/auth/me` | - | mesma resposta do login (401 se não logado) |
| POST | `/auth/logout` | - | 204 |
| POST | `/auth/senha` | `{ "senhaAtual", "novaSenha" }` | 204 |

`permissoes` lista o que o usuário pode fazer, para montar o menu: `CADASTRAR_CLIENTE_VEICULO`,
`CADASTRAR_SERVICO_PECA_MECANICO`, `ABRIR_OS`, `VINCULAR_ITENS_OS`, `ALTERAR_VALOR_ORCAMENTO`,
`ATUALIZAR_STATUS_OS`, `EMITIR_ORCAMENTO_RECIBO`, `CONSULTAR_HISTORICO`, `VISUALIZAR_DASHBOARD`,
`GERENCIAR_USUARIOS`. Usuário inicial: `admin` / `admin123`.

## Erros

Sempre no formato `{ "status": 400, "mensagem": "texto para mostrar ao usuário" }`.

| Status | Quando |
|---|---|
| 400 | dado inválido, regra de negócio violada, estado incorreto, estoque insuficiente, JSON malformado |
| 401 | sem login ou login/senha inválidos |
| 403 | perfil sem permissão ou operação não permitida |
| 404 | registro não encontrado |
| 409 | valor duplicado no banco (documento, placa ou login) |

## Clientes (Gestor, Atendente)

| Método | Rota | Corpo |
|---|---|---|
| GET | `/clientes?busca=&somenteAtivos=` | - |
| POST | `/clientes` | `{ nome, documento, telefone, email, endereco }` |
| GET | `/clientes/{id}` | - |
| PUT | `/clientes/{id}` | `{ nome, telefone, email, endereco }` |
| POST | `/clientes/{id}/inativar` / `/reativar` | - |
| GET | `/clientes/{id}/veiculos` | - |

## Veículos (consulta: todos; alteração: Gestor, Atendente)

| Método | Rota | Corpo |
|---|---|---|
| GET | `/veiculos?busca=` (placa) | - |
| POST | `/veiculos` | `{ placa, marca, modelo, ano, quilometragem, clienteId }` |
| GET | `/veiculos/{id}` | - |
| PUT | `/veiculos/{id}/quilometragem` | `{ quilometragem }` |
| POST | `/veiculos/{id}/transferir` | `{ clienteId }` |
| GET | `/veiculos/{id}/historico` | - |

## Serviços, peças e mecânicos (consulta: todos; alteração: Gestor)

| Método | Rota | Corpo |
|---|---|---|
| GET | `/servicos?busca=&somenteAtivos=` | - |
| POST | `/servicos` | `{ descricao, tempoEstimado, valorMaoDeObra }` |
| PUT | `/servicos/{id}` | `{ tempoEstimado, valorMaoDeObra }` |
| POST | `/servicos/{id}/inativar` / `/reativar` | - |
| GET | `/pecas?busca=&somenteAtivas=` | - |
| POST | `/pecas` | `{ descricao, valorUnitario, quantidadeInicial }` |
| PUT | `/pecas/{id}` | `{ valorUnitario }` |
| POST | `/pecas/{id}/estoque` | `{ quantidade }` (reposição) |
| POST | `/pecas/{id}/inativar` / `/reativar` | - |
| GET | `/mecanicos?somenteAtivos=` | - |
| POST | `/mecanicos` | `{ nome, login, senha, especialidade, telefone }` |
| PUT | `/mecanicos/{id}` | `{ especialidade, telefone }` |
| POST | `/mecanicos/{id}/inativar` / `/reativar` | - |

## Usuários (Gestor)

| Método | Rota | Corpo |
|---|---|---|
| GET | `/usuarios` | - |
| POST | `/usuarios` | `{ nome, login, senha, perfil }` com `perfil` = `GESTOR` ou `ATENDENTE` |
| POST | `/usuarios/{id}/inativar` / `/reativar` | - |

## Ordens de serviço

Todas as ações devolvem a OS atualizada (`OrdemDeServicoResp`), que traz também indicadores prontos para o
front decidir os botões: `aceitaAlteracaoDeItens`, `emExecucao`, `concluida`, `orcamentoAprovado`,
`orcamentoPendente`. O mecânico só enxerga as OS designadas a ele.

| Método | Rota | Corpo | Permissão |
|---|---|---|---|
| GET | `/os?clienteId=&veiculoId=&status=&inicio=&fim=` | - | todos |
| GET | `/os/{id}` | - | todos |
| POST | `/os` | `{ clienteId, veiculoId, problemaRelatado, quilometragem }` | ABRIR_OS |
| PUT | `/os/{id}/mecanico` | `{ mecanicoId }` | ABRIR_OS |
| PUT | `/os/{id}/diagnostico` | `{ diagnostico }` | ATUALIZAR_STATUS_OS |
| POST | `/os/{id}/servicos` | `{ servicoId, valor }` (valor opcional) | VINCULAR_ITENS_OS |
| POST | `/os/{id}/pecas` | `{ pecaId, quantidade }` | VINCULAR_ITENS_OS |
| POST | `/os/{id}/orcamento/gerar` | - | EMITIR_ORCAMENTO_RECIBO |
| POST | `/os/{id}/orcamento/aprovar` | - | EMITIR_ORCAMENTO_RECIBO |
| POST | `/os/{id}/orcamento/recusar` | - | EMITIR_ORCAMENTO_RECIBO |
| PUT | `/os/{id}/valor` | `{ valor }` | ALTERAR_VALOR_ORCAMENTO (Gestor) |
| PUT | `/os/{id}/status` | `{ status }` = `AGUARDANDO_PECA` ou `EM_ANDAMENTO` | ATUALIZAR_STATUS_OS |
| POST | `/os/{id}/servicos/{itemId}/finalizar` | - | ATUALIZAR_STATUS_OS |
| POST | `/os/{id}/finalizar` | - | EMITIR_ORCAMENTO_RECIBO |
| POST | `/os/{id}/comprovante` | - | EMITIR_ORCAMENTO_RECIBO → `{ texto }` |
| POST | `/os/{id}/entregar` | - | EMITIR_ORCAMENTO_RECIBO |

Valores de `status`: `ABERTA`, `EM_ANDAMENTO`, `AGUARDANDO_PECA`, `CONCLUIDA`, `ENTREGUE`.

Ao vincular uma peça sem estoque, a API responde 400, mas a OS passa para `AGUARDANDO_PECA` (RN04): o front
deve recarregar a OS (`GET /os/{id}`) depois do erro.

## Painel (Gestor, Atendente)

`GET /painel` → `{ emAberto: [OrdemResumoResp], porStatus: [{ status, descricao, quantidade }] }`.
Para o Atendente o campo `valorTotal` vem `null` (painel parcial).


## Implementação REST

Os endpoints documentados acima são implementados por controllers em
`com.ifsp.mecplus.web.api`. Os controllers MVC antigos permanecem em
`com.ifsp.mecplus.web` e continuam atendendo às rotas sem o prefixo `/api`.
As respostas REST utilizam os records de `web.dto.Respostas`, evitando expor
diretamente as entidades JPA.
