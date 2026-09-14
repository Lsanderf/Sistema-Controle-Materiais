package com.Lucca.Projeto1;

import com.Lucca.Projeto1.storage.EvidenciaStorageLocal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenciaStorageLocalTest {

    @TempDir
    Path diretorioTemporario;

    private EvidenciaStorageLocal storage;

    @BeforeEach
    void preparar() {
        storage = new EvidenciaStorageLocal(
                diretorioTemporario
                        .resolve("evidencias")
                        .toString()
        );
    }

    @Test
    void armazenarECarregarArquivoMantemConteudoOriginal()
            throws Exception {

        // ARRANGE
        String storageKey =
                "movimentacoes/1/assinatura/teste.png";

        byte[] conteudoOriginal =
                "conteudo-de-teste".getBytes();


        // ACT
        storage.armazenar(storageKey, conteudoOriginal);

        Resource recurso = storage.carregar(storageKey);

        byte[] conteudoCarregado;

        try (var input = recurso.getInputStream()) {
            conteudoCarregado = input.readAllBytes();
        }


        // ASSERT
        assertTrue(recurso.exists());

        assertArrayEquals(
                conteudoOriginal,
                conteudoCarregado
        );
    }

    @Test
    void removerRealmenteApagaArquivo()
            throws Exception {

        // ARRANGE
        String storageKey =
                "movimentacoes/1/assinatura/teste.png";

        byte[] conteudo =
                "arquivo-para-remover".getBytes();

        storage.armazenar(storageKey, conteudo);

        Resource antesDeRemover =
                storage.carregar(storageKey);

        assertTrue(antesDeRemover.exists());


        // ACT
        storage.remover(storageKey);


        // ASSERT
        assertThrows(
                IllegalStateException.class,
                () -> storage.carregar(storageKey)
        );
    }

    @Test
    void storageKeyNaoPodeEscaparDoDiretorioDeEvidencias() {

        byte[] conteudo =
                "conteudo-malicioso".getBytes();

        Path arquivoFora =
                diretorioTemporario.resolve("fora.txt");


        assertThrows(
                IllegalArgumentException.class,
                () -> storage.armazenar(
                        "../fora.txt",
                        conteudo
                )
        );

        assertFalse(
                Files.exists(arquivoFora)
        );
    }
}