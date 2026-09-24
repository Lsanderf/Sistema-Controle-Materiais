# Configuração e API

## Variáveis de Ambiente

Configure as variáveis abaixo antes de iniciar a aplicação. Não crie `.env` com segredos reais no repositório.

Consulte `.env.example` para um modelo sem credenciais reais.

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`, com no mínimo 32 caracteres
- `JWT_EXPIRATION_SECONDS`, por exemplo `3600`
- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`
- `APP_ADMIN_NOME`, opcional; padrão `Administrador`
- `APP_ADMIN_CPF`, opcional; padrão apenas para inicialização local
- `APP_ADMIN_CELULAR`, opcional; padrão apenas para inicialização local
- `APP_CORS_ALLOWED_ORIGINS`, lista separada por vírgula, sem usar `*`
- `APP_EVIDENCIAS_STORAGE`, opcional; nesta versão deve permanecer `local`
- `APP_EVIDENCIAS_DIRETORIO`, opcional; padrão `./data/evidencias`
- `APP_EVIDENCIAS_TAMANHO_MAXIMO`, opcional; padrão `2MB`

O usuário administrador inicial só é criado quando a tabela de usuários está vazia e `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` estão configurados.

## Autenticação

Login público:

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "senha_configurada"
}
```

Resposta:

```json
{
  "token": "jwt",
  "tipo": "Bearer",
  "expiraEm": 3600,
  "role": "ADMIN"
}
```

Use o token nas demais chamadas:

```http
Authorization: Bearer jwt
```

Gerenciamento de usuários, somente `ADMIN`:

```http
GET /usuarios
GET /usuarios/{id}
POST /usuarios
PUT /usuarios/{id}
PATCH /usuarios/{id}/ativar
PATCH /usuarios/{id}/desativar
Authorization: Bearer jwt
```

O cadastro recebe:

```json
{
  "nome": "Operador de Estoque",
  "cpf": "123.456.789-09",
  "celular": "(11) 99999-0000",
  "username": "operador1",
  "password": "umaSenhaSegura",
  "role": "OPERADOR"
}
```

A edição recebe username, role e, opcionalmente, `novaSenha`:

```json
{
  "username": "operador_estoque",
  "role": "OPERADOR",
  "novaSenha": "novaSenhaSegura"
}
```

Sem `novaSenha`, a senha atual é mantida. As respostas administrativas expõem
`id`, `nome`, `celular`, `username`, `role`, `ativo` e `dataInativacao`. CPF,
senha e hash nunca são serializados.

Cadastro e listagem operacional de encarregados:

```http
POST /usuarios/encarregados  # ADMIN
GET /usuarios/encarregados   # ADMIN, OPERADOR ou GERENTE
Authorization: Bearer jwt
```

O cadastro recebe `nome`, `cpf`, `celular`, `username` e `password`. A role é
sempre definida pelo backend como `ENCARREGADO`. A resposta usa um DTO próprio
com `id`, `nome`, `celular`, `username` e `ativo`, sem CPF ou senha.

Papéis disponíveis: `ADMIN`, `OPERADOR`, `GERENTE`, `ENCARREGADO`.

## Autorização

- `/auth/login` e `OPTIONS /**`: público.
- `GET /materiais/**`: `ADMIN`, `OPERADOR` e `GERENTE`; a consulta pelo GERENTE serve para preencher requisições.
- `GET /movimentacoes/**`: `ADMIN` e `OPERADOR`.
- `GET /notas-fiscais/**`: `ADMIN` e `OPERADOR`.
- `GET /contratos/**`: `ADMIN`, `OPERADOR` e `GERENTE`.
- Criação, edição, ativação, desativação e exclusão de contratos: `ADMIN`.
- `POST /movimentacoes`: `ADMIN` e `OPERADOR`.
- `POST /movimentacoes/{id}/assinatura`: `ADMIN` e `OPERADOR`.
- `GET /movimentacoes/{id}/comprovante` e leitura de evidências: `ADMIN` e `OPERADOR`.
- `POST /notas-fiscais` e `POST /notas-fiscais/{id}/confirmar`: `ADMIN` e `OPERADOR`.
- Alterações em materiais: `ADMIN`, exceto o cadastro já permitido a `OPERADOR`.
- `POST /requisicoes`: `GERENTE`; o solicitante vem do JWT.
- `GET /requisicoes` e `GET /requisicoes/{id}`: `ADMIN`, `GERENTE` e `ENCARREGADO`, filtrados e validados pelo usuário autenticado.
- `PATCH /requisicoes/{id}/visualizar` e `/concluir`: somente o `ENCARREGADO` destinatário; `/cancelar`: somente o `GERENTE` solicitante.
- `GERENTE` consulta materiais, contratos e encarregados para criar requisições, mas não acessa movimentações nem altera estoque.
- `ENCARREGADO` acessa apenas as requisições destinadas a ele, sem acesso geral aos módulos administrativos ou operacionais acima.
- Administração geral em `/usuarios/**`: `ADMIN`, exceto os endpoints específicos de encarregados descritos acima.
- Endpoints não configurados exigem autenticação.

O status e a role atuais do usuário são consultados no banco durante cada
requisição autenticada. Assim, tokens já emitidos deixam de funcionar quando a
conta é desativada ou tem suas permissões alteradas.

## Responsável pela Movimentação

Entradas geradas por nota fiscal, retiradas e devoluções associam
automaticamente a movimentação ao `Usuario` autenticado. Os requests não
aceitam `usuarioId`, username ou outro campo que permita escolher o
responsável.

As respostas de movimentação incluem:

```json
{
  "usuarioId": 2,
  "usuarioUsername": "operador1"
}
```

Registros anteriores à associação retornam ambos os campos como `null`.

## Comprovantes e Evidências

Cada movimentação concluída gera, na mesma transação, um snapshot em
`tb_comprovantes_movimentacao`. O snapshot preserva os dados exibidos no
comprovante mesmo que material, funcionário, contrato ou usuário sejam
renomeados no futuro. A migration também gera snapshots para movimentações já
existentes.

Consulta do comprovante:

```http
GET /movimentacoes/{id}/comprovante
Authorization: Bearer jwt
```

A resposta é um DTO próprio e contém os dados da movimentação, material,
funcionário, contrato, operador, Nota Fiscal (para entradas) e a lista de
evidências. CPF e senha não fazem parte desse DTO.

`POST /movimentacoes` também aceita o campo opcional `observacao`, com até
1.000 caracteres. A data de finalização e a observação são adicionadas à
resposta já existente sem remover nenhum campo anterior.

Envio de assinatura para retirada ou devolução:

```http
POST /movimentacoes/{id}/assinatura
Authorization: Bearer jwt
Content-Type: multipart/form-data

arquivo: assinatura.png
```

São aceitas imagens PNG e JPEG de até 2 MB por padrão. O formato é validado
pelos bytes do arquivo, não apenas pelo `Content-Type` informado pelo cliente.
Cada movimentação pode receber uma única assinatura e não há endpoint de
substituição ou exclusão.

O arquivo fica fora do banco. `tb_evidencias_movimentacao` armazena somente
uma chave opaca de armazenamento, nome original, tipo de conteúdo, tamanho,
SHA-256, data/hora, funcionário e usuário que anexou. O download autenticado é:

```http
GET /movimentacoes/{movimentacaoId}/evidencias/{evidenciaId}/arquivo
Authorization: Bearer jwt
```

O serviço usa a interface `EvidenciaStorage`; a implementação atual grava em
disco local. Para S3 ou MinIO, implemente a mesma interface, copie os objetos
preservando as chaves e selecione o novo bean por configuração. Nenhuma imagem
é armazenada como Base64 em coluna textual.

No PostgreSQL, triggers da migration V8 bloqueiam alteração e exclusão das
movimentações finalizadas, dos snapshots e dos metadados das evidências. O
Hibernate também trata essas entidades como imutáveis. Fotos de devolução já
estão previstas pelo tipo `FOTO_DEVOLUCAO`, mas ainda não possuem endpoint de
upload nesta etapa.

## Regras de Estoque

- `POST /materiais` cadastra material novo com `quantidadeEstoque = 0`.
- O request de cadastro de material recebe apenas `nome` e `descricao`.
- O estoque só aumenta pela confirmação de uma nota fiscal de entrada em `POST /notas-fiscais/{id}/confirmar`.
- `POST /movimentacoes` aceita somente `RETIRADA` e `DEVOLUCAO`; `ENTRADA` é rejeitada nesse endpoint.
- Não existe endpoint público para entrada manual de estoque.
- Cada operação aceita no máximo 10.000 unidades.
- O estoque não pode ficar negativo.
- Alterações de estoque usam bloqueio pessimista do material dentro da mesma transação da movimentação.

Exemplo de cadastro de material:

```json
{
  "nome": "Capacete de segurança",
  "descricao": "Capacete para uso em obra"
}
```

## Observações Sobre Banco Existente

A migration `V2__seguranca_constraints_e_indices.sql` cria a tabela de usuários, limita movimentações a 10.000 unidades e adiciona índices únicos para:

- username de usuário ignorando maiúsculas/minúsculas;
- nome de material ignorando maiúsculas/minúsculas;
- nome de contrato ignorando maiúsculas/minúsculas;
- CPF normalizado, sem pontos, traços ou espaços.

Se o banco atual já tiver dados duplicados que violem essas regras, a migration falhará. Corrija esses registros manualmente antes de aplicar a migration.

A migration `V3__adicionar_usuario_responsavel_movimentacao.sql` adiciona
`usuario_id` e sua chave estrangeira a `tb_movimentacoes`. A coluna permanece
aceitando `null` para preservar movimentações antigas; nenhum usuário é
atribuído retroativamente.
