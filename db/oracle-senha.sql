-- Rode no Oracle antes de subir a API.
-- A carga da Global Solution não tem senha. A coluna entra nula
-- e, na primeira subida, a API grava o hash de senha123 em quem estiver sem senha.

ALTER TABLE tb_pg_usuario ADD (
    ds_senha VARCHAR2(100)
);
