ALTER TABLE tb_movimentacoes
    ADD COLUMN movimentacao_origem_id BIGINT;

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT fk_movimentacoes_origem
        FOREIGN KEY (movimentacao_origem_id)
            REFERENCES tb_movimentacoes(id);

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT uk_movimentacoes_origem
        UNIQUE (movimentacao_origem_id);

ALTER TABLE tb_movimentacoes
    DROP CONSTRAINT ck_movimentacoes_tipo;

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT ck_movimentacoes_tipo
        CHECK (tipo IN (
            'ENTRADA',
            'RETIRADA',
            'DEVOLUCAO',
            'ESTORNO_RETIRADA',
            'ESTORNO_DEVOLUCAO'
        ));

ALTER TABLE tb_movimentacoes
    ADD CONSTRAINT ck_movimentacoes_origem_estorno
        CHECK (
            (tipo IN ('ESTORNO_RETIRADA', 'ESTORNO_DEVOLUCAO')
                AND movimentacao_origem_id IS NOT NULL)
            OR
            (tipo IN ('ENTRADA', 'RETIRADA', 'DEVOLUCAO')
                AND movimentacao_origem_id IS NULL)
        );

ALTER TABLE tb_comprovantes_movimentacao
    ADD COLUMN movimentacao_origem_id BIGINT;

ALTER TABLE tb_comprovantes_movimentacao
    ADD CONSTRAINT fk_comprovantes_movimentacao_origem
        FOREIGN KEY (movimentacao_origem_id)
            REFERENCES tb_movimentacoes(id);

ALTER TABLE tb_comprovantes_movimentacao
    DROP CONSTRAINT ck_comprovantes_tipo;

ALTER TABLE tb_comprovantes_movimentacao
    ADD CONSTRAINT ck_comprovantes_tipo
        CHECK (tipo IN (
            'ENTRADA',
            'RETIRADA',
            'DEVOLUCAO',
            'ESTORNO_RETIRADA',
            'ESTORNO_DEVOLUCAO'
        ));

CREATE OR REPLACE FUNCTION fn_validar_movimentacao_estorno()
RETURNS TRIGGER AS $$
DECLARE
    origem tb_movimentacoes%ROWTYPE;
BEGIN
    IF NEW.tipo IN ('ESTORNO_RETIRADA', 'ESTORNO_DEVOLUCAO') THEN
        SELECT *
        INTO origem
        FROM tb_movimentacoes
        WHERE id = NEW.movimentacao_origem_id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Movimentação de origem do estorno não encontrada'
                USING ERRCODE = '23503';
        END IF;

        IF (NEW.tipo = 'ESTORNO_RETIRADA' AND origem.tipo <> 'RETIRADA')
            OR (NEW.tipo = 'ESTORNO_DEVOLUCAO' AND origem.tipo <> 'DEVOLUCAO')
            OR NEW.quantidade IS DISTINCT FROM origem.quantidade
            OR NEW.material_id IS DISTINCT FROM origem.material_id
            OR NEW.funcionario_id IS DISTINCT FROM origem.funcionario_id
            OR NEW.contrato_id IS DISTINCT FROM origem.contrato_id
            OR NEW.nota_fiscal_id IS NOT NULL THEN
            RAISE EXCEPTION 'O estorno deve reproduzir integralmente os dados da movimentação de origem'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_movimentacao_estorno
BEFORE INSERT ON tb_movimentacoes
FOR EACH ROW
EXECUTE FUNCTION fn_validar_movimentacao_estorno();

CREATE OR REPLACE FUNCTION fn_bloquear_alteracao_movimentacao_finalizada()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Movimentações finalizadas não podem ser excluídas'
            USING ERRCODE = '55000';
    END IF;

    IF OLD.data_finalizacao IS NOT NULL AND (
        OLD.funcionario_id IS DISTINCT FROM NEW.funcionario_id
        OR OLD.contrato_id IS DISTINCT FROM NEW.contrato_id
        OR OLD.material_id IS DISTINCT FROM NEW.material_id
        OR OLD.quantidade IS DISTINCT FROM NEW.quantidade
        OR OLD.tipo IS DISTINCT FROM NEW.tipo
        OR OLD.data_movimentacao IS DISTINCT FROM NEW.data_movimentacao
        OR OLD.data_finalizacao IS DISTINCT FROM NEW.data_finalizacao
        OR OLD.usuario_id IS DISTINCT FROM NEW.usuario_id
        OR OLD.nota_fiscal_id IS DISTINCT FROM NEW.nota_fiscal_id
        OR OLD.observacao IS DISTINCT FROM NEW.observacao
        OR OLD.idempotency_key IS DISTINCT FROM NEW.idempotency_key
        OR OLD.request_fingerprint IS DISTINCT FROM NEW.request_fingerprint
        OR OLD.movimentacao_origem_id IS DISTINCT FROM NEW.movimentacao_origem_id
    ) THEN
        RAISE EXCEPTION 'Dados críticos de uma movimentação finalizada não podem ser alterados'
            USING ERRCODE = '55000';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
