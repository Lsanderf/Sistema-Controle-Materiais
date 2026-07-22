ALTER TABLE tb_funcionarios
    ADD COLUMN ativo BOOLEAN;

UPDATE tb_funcionarios
SET ativo = TRUE
WHERE ativo IS NULL;

ALTER TABLE tb_funcionarios
    ALTER COLUMN ativo SET NOT NULL;