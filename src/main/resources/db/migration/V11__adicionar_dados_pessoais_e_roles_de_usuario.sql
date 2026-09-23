ALTER TABLE tb_usuarios
    ADD COLUMN nome VARCHAR(150);

ALTER TABLE tb_usuarios
    ADD COLUMN cpf VARCHAR(11);

ALTER TABLE tb_usuarios
    ADD COLUMN celular VARCHAR(20);

ALTER TABLE tb_usuarios
    DROP CONSTRAINT ck_usuarios_role;

-- Registros anteriores não possuíam dados pessoais. Os valores técnicos abaixo
-- mantêm bancos existentes migráveis; novos cadastros passam pelas validações da API.
UPDATE tb_usuarios
SET nome = BTRIM(username),
    cpf = LPAD(CAST(id AS VARCHAR), 11, '0'),
    celular = '00000000000',
    role = CASE
        WHEN role IN ('ADMIN', 'OPERADOR', 'GERENTE', 'ENCARREGADO') THEN role
        ELSE 'GERENTE'
    END;

ALTER TABLE tb_usuarios
    ALTER COLUMN nome SET NOT NULL;

ALTER TABLE tb_usuarios
    ALTER COLUMN cpf SET NOT NULL;

ALTER TABLE tb_usuarios
    ALTER COLUMN celular SET NOT NULL;

ALTER TABLE tb_usuarios
    ADD CONSTRAINT ck_usuarios_role
        CHECK (role IN ('ADMIN', 'OPERADOR', 'GERENTE', 'ENCARREGADO'));

ALTER TABLE tb_usuarios
    ADD CONSTRAINT ck_usuarios_nome_nao_vazio
        CHECK (BTRIM(nome) <> '');

ALTER TABLE tb_usuarios
    ADD CONSTRAINT ck_usuarios_cpf_11_digitos
        CHECK (cpf ~ '^[0-9]{11}$');

ALTER TABLE tb_usuarios
    ADD CONSTRAINT ck_usuarios_celular_digitos
        CHECK (celular ~ '^[0-9]{8,20}$');

ALTER TABLE tb_usuarios
    ADD CONSTRAINT uk_usuarios_cpf UNIQUE (cpf);
