package com.Lucca.Projeto1.storage;

import org.springframework.core.io.Resource;

public interface EvidenciaStorage {

    void armazenar(String storageKey, byte[] conteudo);

    Resource carregar(String storageKey);

    void remover(String storageKey);
}
