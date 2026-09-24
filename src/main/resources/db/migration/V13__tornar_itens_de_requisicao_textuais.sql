ALTER TABLE tb_requisicao_itens
    ADD COLUMN descricao VARCHAR(255);

UPDATE tb_requisicao_itens item
SET descricao = CASE
    WHEN BTRIM(material.nome) <> '' THEN material.nome
    ELSE 'Material legado #' || item.material_id
END
FROM tb_materiais material
WHERE material.id = item.material_id;

ALTER TABLE tb_requisicao_itens
    ALTER COLUMN descricao SET NOT NULL;

ALTER TABLE tb_requisicao_itens
    ADD CONSTRAINT ck_requisicao_itens_descricao_nao_vazia
        CHECK (BTRIM(descricao) <> '');

ALTER TABLE tb_requisicao_itens
    DROP CONSTRAINT uk_requisicao_itens_material;

ALTER TABLE tb_requisicao_itens
    DROP CONSTRAINT fk_requisicao_itens_material;

ALTER TABLE tb_requisicao_itens
    DROP COLUMN material_id;
