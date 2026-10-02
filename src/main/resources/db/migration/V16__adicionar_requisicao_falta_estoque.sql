ALTER TABLE tb_requisicoes
    ALTER COLUMN gerente_solicitante_id DROP NOT NULL;

ALTER TABLE tb_requisicoes
    ALTER COLUMN encarregado_destinatario_id DROP NOT NULL;

ALTER TABLE tb_requisicoes
    ADD COLUMN origem VARCHAR(30) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE tb_requisicoes
    ADD CONSTRAINT ck_requisicoes_origem CHECK (origem IN ('MANUAL', 'FALTA_ESTOQUE'));

ALTER TABLE tb_requisicoes
    ADD COLUMN operador_registrador_id BIGINT;

ALTER TABLE tb_requisicoes
    ADD COLUMN encarregado_necessidade_id BIGINT;

ALTER TABLE tb_requisicoes
    ADD COLUMN gerente_destinatario_id BIGINT;

ALTER TABLE tb_requisicoes
    ADD CONSTRAINT fk_requisicoes_operador_registrador
        FOREIGN KEY (operador_registrador_id) REFERENCES tb_usuarios(id);

ALTER TABLE tb_requisicoes
    ADD CONSTRAINT fk_requisicoes_encarregado_necessidade
        FOREIGN KEY (encarregado_necessidade_id) REFERENCES tb_usuarios(id);

ALTER TABLE tb_requisicoes
    ADD CONSTRAINT fk_requisicoes_gerente_destinatario
        FOREIGN KEY (gerente_destinatario_id) REFERENCES tb_usuarios(id);

CREATE INDEX idx_requisicoes_gerente_destinatario
    ON tb_requisicoes(gerente_destinatario_id, status, criada_em DESC);

CREATE INDEX idx_requisicoes_operador_registrador
    ON tb_requisicoes(operador_registrador_id, criada_em DESC);

ALTER TABLE tb_requisicao_itens
    ADD COLUMN quantidade_solicitada INTEGER;

ALTER TABLE tb_requisicao_itens
    ADD COLUMN quantidade_disponivel INTEGER;

ALTER TABLE tb_requisicao_itens
    ADD COLUMN quantidade_faltante INTEGER;

ALTER TABLE tb_requisicao_itens
    ADD CONSTRAINT ck_requisicao_itens_estoque_nao_negativo
        CHECK (quantidade_disponivel IS NULL OR quantidade_disponivel >= 0);
