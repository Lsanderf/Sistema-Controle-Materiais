# Plano de implementação — retirada assinada

Especificação de referência: aprovação do usuário nesta conversa, em 2026-09-29.

## Restrições globais

- Não criar nem usar `Funcionario` no novo fluxo.
- Preservar movimentações, NF-e, devolução, estorno e requisições legadas.
- Não alterar migrations já existentes.
- Preservar as alterações locais existentes no frontend.
- Todo comportamento novo começa por teste que falha.

## Tarefa 1 — Aggregate de solicitação e criação

Criar a migration V14 e os modelos, DTOs, repositórios, serviço e endpoints mínimos de
`SolicitacaoRetirada`. A criação deve obter o operador do JWT, validar encarregado, contrato
quando informado e todos os materiais, validar estoque sem baixá-lo, e ser idempotente.

Teste: criação bem-sucedida, operador/enacarregado auditados, estoque inalterado e erro 409
estruturado quando faltar material.

## Tarefa 2 — Confirmação assinada e movimentações atômicas

Adicionar assinatura vinculada à solicitação e confirmação por encarregado destinatário. Reutilizar
validador/armazenamento de evidências, aplicar locks em ordem, criar uma retirada por item,
baixar estoque atomicamente e tornar confirmações repetidas idempotentes.

Teste: autorização, assinatura, uma baixa, repetição, concorrência e falta posterior de estoque.

## Tarefa 3 — Requisição de compra por falta de estoque

Generalizar o solicitante da requisição sem alterar o fluxo manual GERENTE → ENCARREGADO.
Adicionar origem `FALTA_ESTOQUE`, encarregado relacionado e responsável gerente destinatário,
mais endpoint exclusivo para ADMIN/OPERADOR que recalcula a falta no backend.

Teste: a requisição contém operador, encarregado, gerente destinatário e somente a quantidade
faltante; a criação manual legada permanece igual.

## Tarefa 4 — Compatibilidade operacional

Adaptar devolução ao encarregado/usuário sem quebrar histórico baseado em funcionário. Impedir
retirada direta por `/movimentacoes`; preservar NF-e, estorno e registros históricos.

Teste: devolução de retirada nova e estorno de retirada confirmada; regressões NF-e e legado.

## Tarefa 5 — Frontend

Criar o fluxo de preparação/listagem/confirmação mobile, os serviços e o acionamento de
requisição de compra. Manter a tela de devolução e preservar alterações locais de CSS/Feedback.

Teste: payload sem `funcionarioId`, feedback de solicitação pendente, painel do encarregado,
confirmação e falta de estoque.

## Tarefa 6 — Verificação final

Executar suites completas backend e frontend, revisar o diff, registrar riscos e resultados.
