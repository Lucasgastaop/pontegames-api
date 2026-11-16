-- Rode no Oracle antes de subir a API.
-- O parecer da foto não existia na entrega de database.
-- A coluna fica nula quando o descarte não tem foto.

ALTER TABLE tb_pg_descarte ADD (
    tp_parecer_ia VARCHAR2(20),
    ds_parecer_ia VARCHAR2(500)
);
