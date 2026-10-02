package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.model.AssinaturaRetirada;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.SolicitacaoRetirada;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.AssinaturaRetiradaRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.storage.EvidenciaStorage;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssinaturaRetiradaService {
    private final AssinaturaRetiradaRepository assinaturaRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final EvidenciaStorage storage;

    public AssinaturaRetiradaService(
            AssinaturaRetiradaRepository assinaturaRepository,
            MovimentacaoRepository movimentacaoRepository,
            EvidenciaStorage storage
    ) {
        this.assinaturaRepository = assinaturaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.storage = storage;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(SolicitacaoRetirada solicitacao, Usuario encarregado, ImagemEvidenciaValidator.ImagemValidada imagem) {
        if (assinaturaRepository.existsBySolicitacaoRetiradaId(solicitacao.getId())) return;
        String storageKey = "solicitacoes-retirada/" + solicitacao.getId() + "/assinatura/" + UUID.randomUUID() + "." + imagem.extensao();
        registrarLimpezaEmCasoDeRollback(storageKey);
        try {
            storage.armazenar(storageKey, imagem.conteudo());
            AssinaturaRetirada assinatura = new AssinaturaRetirada();
            assinatura.setSolicitacaoRetirada(solicitacao);
            assinatura.setEncarregadoAssinante(encarregado);
            assinatura.setDataAssinatura(LocalDateTime.now());
            assinatura.setStorageKey(storageKey);
            assinatura.setNomeArquivoOriginal(normalizarNomeArquivo(imagem.nomeOriginal(), imagem.extensao()));
            assinatura.setContentType(imagem.contentType());
            assinatura.setTamanhoBytes((long) imagem.conteudo().length);
            assinatura.setSha256(sha256(imagem.conteudo()));
            assinaturaRepository.saveAndFlush(assinatura);
        } catch (RuntimeException exception) {
            removerSilenciosamente(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Optional<AssinaturaRetirada> buscarPorSolicitacao(Long solicitacaoId) {
        if (solicitacaoId == null) return Optional.empty();
        return assinaturaRepository.findBySolicitacaoRetiradaId(solicitacaoId);
    }

    @Transactional(readOnly = true)
    public ArquivoAssinatura buscarArquivo(Long movimentacaoId) {
        Movimentacao movimentacao = movimentacaoRepository.findById(movimentacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Movimentação com ID " + movimentacaoId + " não encontrada"
                ));
        SolicitacaoRetirada solicitacao = movimentacao.getSolicitacaoRetirada();
        if (solicitacao == null) {
            throw assinaturaNaoEncontrada();
        }
        AssinaturaRetirada assinatura = assinaturaRepository
                .findBySolicitacaoRetiradaId(solicitacao.getId())
                .orElseThrow(this::assinaturaNaoEncontrada);
        Resource recurso = storage.carregar(assinatura.getStorageKey());
        validarIntegridade(recurso, assinatura);
        return new ArquivoAssinatura(
                recurso,
                assinatura.getContentType(),
                assinatura.getNomeArquivoOriginal(),
                assinatura.getTamanhoBytes(),
                assinatura.getSha256()
        );
    }

    private void registrarLimpezaEmCasoDeRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) removerSilenciosamente(storageKey);
            }
        });
    }

    private String normalizarNomeArquivo(String nomeOriginal, String extensao) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) return "assinatura." + extensao;
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        return nome.isBlank() ? "assinatura." + extensao : nome.substring(0, Math.min(255, nome.length()));
    }

    private String sha256(byte[] conteudo) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 indisponível", exception); }
    }

    private void validarIntegridade(Resource recurso, AssinaturaRetirada assinatura) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }

        long quantidadeLida = 0;
        try (
                InputStream input = recurso.getInputStream();
                DigestInputStream inputComHash = new DigestInputStream(input, digest)
        ) {
            byte[] buffer = new byte[8192];
            int bytesLidos;
            while ((bytesLidos = inputComHash.read(buffer)) != -1) {
                quantidadeLida += bytesLidos;
                if (quantidadeLida > assinatura.getTamanhoBytes()) {
                    throw new IllegalStateException("A assinatura falhou na verificação de integridade");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível validar o arquivo da assinatura", exception);
        }

        String hash = HexFormat.of().formatHex(digest.digest());
        if (quantidadeLida != assinatura.getTamanhoBytes() || !hash.equals(assinatura.getSha256())) {
            throw new IllegalStateException("A assinatura falhou na verificação de integridade");
        }
    }

    private RecursoNaoEncontradoException assinaturaNaoEncontrada() {
        return new RecursoNaoEncontradoException(
                "Assinatura de solicitação não encontrada para a movimentação informada"
        );
    }

    private void removerSilenciosamente(String storageKey) {
        try { storage.remover(storageKey); } catch (RuntimeException ignored) { }
    }

    public record ArquivoAssinatura(
            Resource recurso,
            String contentType,
            String nomeArquivo,
            Long tamanhoBytes,
            String sha256
    ) {
    }
}
