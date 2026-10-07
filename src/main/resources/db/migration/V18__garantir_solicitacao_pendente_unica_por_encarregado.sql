CREATE UNIQUE INDEX uk_solicitacoes_retirada_encarregado_pendente
    ON tb_solicitacoes_retirada(encarregado_assinante_id)
    WHERE status = 'AGUARDANDO_ASSINATURA';
