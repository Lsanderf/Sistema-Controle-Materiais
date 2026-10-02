ALTER TABLE tb_evidencias_movimentacao
    ALTER COLUMN funcionario_id DROP NOT NULL;

ALTER TABLE tb_evidencias_movimentacao
    ALTER COLUMN funcionario_nome DROP NOT NULL;

ALTER TABLE tb_evidencias_movimentacao
    ADD COLUMN encarregado_id BIGINT;

ALTER TABLE tb_evidencias_movimentacao
    ADD COLUMN encarregado_nome VARCHAR(150);

ALTER TABLE tb_evidencias_movimentacao
    ADD CONSTRAINT fk_evidencias_encarregado
        FOREIGN KEY (encarregado_id) REFERENCES tb_usuarios(id);

ALTER TABLE tb_evidencias_movimentacao
    ADD CONSTRAINT ck_evidencias_responsavel
        CHECK (
            (funcionario_id IS NOT NULL
                AND funcionario_nome IS NOT NULL
                AND encarregado_id IS NULL
                AND encarregado_nome IS NULL)
            OR
            (funcionario_id IS NULL
                AND funcionario_nome IS NULL
                AND encarregado_id IS NOT NULL
                AND encarregado_nome IS NOT NULL)
        );

CREATE INDEX idx_evidencias_encarregado
    ON tb_evidencias_movimentacao(encarregado_id);
