ALTER TABLE tb_movimentacoes
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE tb_movimentacoes
    ADD COLUMN request_fingerprint VARCHAR(64);

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT uk_movimentacao_usuario_idempotency
        UNIQUE (usuario_id, idempotency_key);