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
    ) THEN
        RAISE EXCEPTION 'Dados críticos de uma movimentação finalizada não podem ser alterados'
            USING ERRCODE = '55000';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bloquear_alteracao_movimentacao_finalizada
BEFORE UPDATE OR DELETE ON tb_movimentacoes
FOR EACH ROW
EXECUTE FUNCTION fn_bloquear_alteracao_movimentacao_finalizada();

CREATE OR REPLACE FUNCTION fn_bloquear_alteracao_registro_imutavel()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Registros de comprovante e evidência são imutáveis'
        USING ERRCODE = '55000';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bloquear_alteracao_comprovante
BEFORE UPDATE OR DELETE ON tb_comprovantes_movimentacao
FOR EACH ROW
EXECUTE FUNCTION fn_bloquear_alteracao_registro_imutavel();

CREATE TRIGGER trg_bloquear_alteracao_evidencia
BEFORE UPDATE OR DELETE ON tb_evidencias_movimentacao
FOR EACH ROW
EXECUTE FUNCTION fn_bloquear_alteracao_registro_imutavel();
