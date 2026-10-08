ALTER TABLE tb_requisicoes
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN atualizada_em TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN atualizada_por_id BIGINT,
    ADD CONSTRAINT fk_requisicoes_atualizada_por
        FOREIGN KEY (atualizada_por_id) REFERENCES tb_usuarios(id);
