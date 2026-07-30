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
- `APP_CORS_ALLOWED_ORIGINS`, lista separada por vírgula, sem usar `*`

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

Cadastro de usuário, somente `ADMIN`:

```http
POST /usuarios
Authorization: Bearer jwt
Content-Type: application/json
```

```json
{
  "username": "operador1",
  "password": "umaSenhaSegura",
  "role": "OPERADOR"
}
```

Papéis disponíveis: `ADMIN`, `OPERADOR`, `CONSULTA`.

## Autorização

- `/auth/login` e `OPTIONS /**`: público.
- `GET /materiais/**`, `GET /funcionarios/**`, `GET /contratos/**` e `GET /movimentacoes/**`: `ADMIN`, `OPERADOR` e `CONSULTA`.
- `POST /movimentacoes` e `POST /movimentacoes/entrada`: `ADMIN` e `OPERADOR`.
- Alterações em materiais, funcionários e contratos: `ADMIN`.
- `/usuarios/**`: `ADMIN`.
- Endpoints não configurados exigem autenticação.

## Regras de Estoque

- `POST /materiais` cadastra material novo com `quantidadeEstoque = 0`.
- O request de cadastro de material recebe apenas `nome` e `descricao`.
- O estoque só aumenta por `POST /movimentacoes/entrada`.
- `POST /movimentacoes` aceita somente `RETIRADA` e `DEVOLUCAO`; `ENTRADA` é rejeitada nesse endpoint.
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
