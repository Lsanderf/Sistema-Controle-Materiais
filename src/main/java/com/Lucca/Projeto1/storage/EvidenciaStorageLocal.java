package com.Lucca.Projeto1.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Component
@ConditionalOnProperty(
        name = "app.evidencias.storage",
        havingValue = "local",
        matchIfMissing = true
)
public class EvidenciaStorageLocal implements EvidenciaStorage {

    private final Path diretorioBase;

    public EvidenciaStorageLocal(
            @Value("${app.evidencias.diretorio:./data/evidencias}")
            String diretorio
    ) {
        diretorioBase = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void armazenar(String storageKey, byte[] conteudo) {
        Path destino = resolver(storageKey);
        Path temporario = null;

        try {
            Files.createDirectories(destino.getParent());
            temporario = Files.createTempFile(
                    destino.getParent(),
                    "evidencia-",
                    ".tmp"
            );
            Files.write(
                    temporario,
                    conteudo,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            try {
                Files.move(
                        temporario,
                        destino,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporario, destino);
            }
        } catch (IOException exception) {
            excluirTemporario(temporario);
            throw new IllegalStateException(
                    "Não foi possível armazenar a evidência",
                    exception
            );
        }
    }

    @Override
    public Resource carregar(String storageKey) {
        Path arquivo = resolver(storageKey);
        if (!Files.isRegularFile(arquivo)) {
            throw new IllegalStateException(
                    "O arquivo da evidência não está disponível"
            );
        }
        return new FileSystemResource(arquivo);
    }

    @Override
    public void remover(String storageKey) {
        try {
            Files.deleteIfExists(resolver(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível remover a evidência não confirmada",
                    exception
            );
        }
    }

    private Path resolver(String storageKey) {
        Path caminho = diretorioBase.resolve(storageKey).normalize();
        if (!caminho.startsWith(diretorioBase)) {
            throw new IllegalArgumentException("Referência de arquivo inválida");
        }
        return caminho;
    }

    private void excluirTemporario(Path temporario) {
        if (temporario == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException ignored) {
            // A falha original de armazenamento é a informação relevante.
        }
    }
}
