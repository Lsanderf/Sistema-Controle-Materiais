ALTER TABLE tb_movimentacoes
    ADD COLUMN usuario_id BIGINT;

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT fk_movimentacoes_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES tb_usuarios(id);

CREATE INDEX idx_movimentacoes_usuario
    ON tb_movimentacoes(usuario_id);
