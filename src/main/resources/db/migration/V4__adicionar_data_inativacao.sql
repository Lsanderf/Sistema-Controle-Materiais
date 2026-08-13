ALTER TABLE tb_usuarios
    ADD COLUMN data_inativacao TIMESTAMP NULL;

ALTER TABLE tb_funcionarios
    ADD COLUMN data_inativacao TIMESTAMP NULL;

ALTER TABLE tb_contratos
    ADD COLUMN data_inativacao TIMESTAMP NULL;
