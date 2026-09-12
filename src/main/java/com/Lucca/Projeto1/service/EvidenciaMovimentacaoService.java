package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.movimentacao.EvidenciaMovimentacaoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.EvidenciaMovimentacaoMapper;
import com.Lucca.Projeto1.model.EvidenciaMovimentacao;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.TipoEvidenciaMovimentacao;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.storage.EvidenciaStorage;
import com.Lucca.Projeto1.service.ImagemEvidenciaValidator.ImagemValidada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class EvidenciaMovimentacaoService {

    private static final Logger log = LoggerFactory.getLogger(EvidenciaMovimentacaoService.class);

    private static final TipoEvidenciaMovimentacao ASSINATURA =
            TipoEvidenciaMovimentacao.ASSINATURA;

    private final MovimentacaoRepository movimentacaoRepository;
    private final EvidenciaMovimentacaoRepository evidenciaRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final EvidenciaStorage storage;
    private final long tamanhoMaximoBytes;
    private final ImagemEvidenciaValidator imagemValidator;

    public EvidenciaMovimentacaoService(
            MovimentacaoRepository movimentacaoRepository,
            EvidenciaMovimentacaoRepository evidenciaRepository,
            UsuarioAutenticadoService usuarioAutenticadoService,
            EvidenciaStorage storage,
            ImagemEvidenciaValidator imagemValidator,
            @Value("${app.evidencias.tamanho-maximo:2MB}") String tamanhoMaximo
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.storage = storage;
        this.imagemValidator = imagemValidator;
        this.tamanhoMaximoBytes = DataSize.parse(tamanhoMaximo).toBytes();
    }

    @Transactional
    public EvidenciaMovimentacaoResponse registrarAssinatura(
            Long movimentacaoId,
            MultipartFile arquivo
    ) {
        Movimentacao movimentacao = movimentacaoRepository
                .findByIdComBloqueio(movimentacaoId)
                .orElseThrow(() -> movimentacaoNaoEncontrada(movimentacaoId));

        validarTipoComAssinatura(movimentacao);
        if (evidenciaRepository.existsByMovimentacaoIdAndTipo(
                movimentacaoId,
                ASSINATURA
        )) {
            throw new RegraNegocioException(
                    "A movimentação já possui uma assinatura"
            );
        }

        return armazenarEvidencia(movimentacao, ASSINATURA, imagemValidator.validar(arquivo, true));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarNaCriacao(
            Movimentacao movimentacao,
            ImagemValidada assinatura,
            ImagemValidada foto
    ) {
        validarTipoComAssinatura(movimentacao);
        if (assinatura == null) {
            throw new RegraNegocioException("O arquivo da assinatura é obrigatório");
        }
        if (foto != null && movimentacao.getTipo() != TipoMovimentacao.DEVOLUCAO) {
            throw new RegraNegocioException("Foto do material é permitida apenas na devolução");
        }
        armazenarEvidencia(movimentacao, ASSINATURA, assinatura);
        if (foto != null) {
            armazenarEvidencia(movimentacao, TipoEvidenciaMovimentacao.FOTO_DEVOLUCAO, foto);
        }
    }

    private EvidenciaMovimentacaoResponse armazenarEvidencia(
            Movimentacao movimentacao,
            TipoEvidenciaMovimentacao tipo,
            ImagemValidada imagem
    ) {
        Long movimentacaoId = movimentacao.getId();
        byte[] conteudo = imagem.conteudo();
        Usuario usuario = usuarioAutenticadoService.obter();
        Funcionario funcionario = movimentacao.getFuncionario();
        String storageKey = "movimentacoes/" + movimentacaoId + "/"
                + tipo.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + "." + imagem.extensao();

        // Register before writing: even a storage implementation that writes then
        // throws must participate in compensating cleanup on transaction rollback.
        registrarLimpezaEmCasoDeRollback(storageKey);

        try {
            storage.armazenar(storageKey, conteudo);
            EvidenciaMovimentacao evidencia = new EvidenciaMovimentacao(
                    movimentacaoId,
                    tipo,
                    LocalDateTime.now(),
                    funcionario.getId(),
                    funcionario.getNome(),
                    usuario.getId(),
                    usuario.getUsername(),
                    storageKey,
                    normalizarNomeArquivo(
                            imagem.nomeOriginal(),
                            imagem.extensao()
                    ),
                    imagem.contentType(),
                    (long) conteudo.length,
                    sha256(conteudo)
            );

            return EvidenciaMovimentacaoMapper.paraResponse(
                    evidenciaRepository.saveAndFlush(evidencia)
            );
        } catch (RuntimeException exception) {
            removerSilenciosamente(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ArquivoEvidencia buscarArquivo(
            Long movimentacaoId,
            Long evidenciaId
    ) {
        if (!movimentacaoRepository.existsById(movimentacaoId)) {
            throw movimentacaoNaoEncontrada(movimentacaoId);
        }

        EvidenciaMovimentacao evidencia = evidenciaRepository
                .findByIdAndMovimentacaoId(evidenciaId, movimentacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Evidência não encontrada para a movimentação informada"
                ));

        Resource recurso = storage.carregar(evidencia.getStorageKey());
        validarIntegridade(recurso, evidencia);

        return new ArquivoEvidencia(
                recurso,
                evidencia.getContentType(),
                evidencia.getNomeArquivoOriginal(),
                evidencia.getTamanhoBytes(),
                evidencia.getSha256()
        );
    }

    private void validarTipoComAssinatura(Movimentacao movimentacao) {
        if (movimentacao.getTipo() != TipoMovimentacao.RETIRADA
                && movimentacao.getTipo() != TipoMovimentacao.DEVOLUCAO) {
            throw new RegraNegocioException(
                    "Assinaturas são permitidas apenas em retiradas e devoluções"
            );
        }
        if (movimentacao.getFuncionario() == null) {
            throw new RegraNegocioException(
                    "A movimentação não possui funcionário relacionado"
            );
        }
    }

    private String normalizarNomeArquivo(String nomeOriginal, String extensao) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "evidencia." + extensao;
        }

        String caminhoNormalizado = nomeOriginal.replace('\\', '/');
        String nome = caminhoNormalizado
                .substring(caminhoNormalizado.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();

        if (nome.isBlank()) {
            return "evidencia." + extensao;
        }
        return nome.substring(0, Math.min(nome.length(), 255));
    }

    private String sha256(byte[] conteudo) {
        return formatarHash(novoSha256().digest(conteudo));
    }

    private void validarIntegridade(
            Resource recurso,
            EvidenciaMovimentacao evidencia
    ) {
        MessageDigest digest = novoSha256();
        long quantidadeLida = 0;

        try (
                InputStream input = recurso.getInputStream();
                DigestInputStream inputComHash = new DigestInputStream(
                        input,
                        digest
                )
        ) {
            byte[] buffer = new byte[8192];
            int bytesLidos;
            while ((bytesLidos = inputComHash.read(buffer)) != -1) {
                quantidadeLida += bytesLidos;
                if (quantidadeLida > tamanhoMaximoBytes) {
                    throw new IllegalStateException(
                            "O arquivo da evidência excede o limite configurado"
                    );
                }
            }

            if (quantidadeLida != evidencia.getTamanhoBytes()
                    || !formatarHash(digest.digest()).equals(
                            evidencia.getSha256()
                    )) {
                throw new IllegalStateException(
                        "O arquivo da evidência falhou na verificação de integridade"
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível validar o arquivo da evidência",
                    exception
            );
        }
    }

    private MessageDigest novoSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 não está disponível",
                    exception
            );
        }
    }

    private String formatarHash(byte[] hash) {
        return HexFormat.of().formatHex(hash).toLowerCase(Locale.ROOT);
    }

    private void registrarLimpezaEmCasoDeRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            removerSilenciosamente(storageKey);
                        } else if (status == STATUS_UNKNOWN) {
                            // Keep the evidence when the commit outcome is unknown:
                            // deleting it could break an already committed movement.
                            log.warn("Resultado transacional desconhecido; evidência preservada: {}", storageKey);
                        }
                    }
                }
        );
    }

    private void removerSilenciosamente(String storageKey) {
        try {
            storage.remover(storageKey);
        } catch (RuntimeException exception) {
            log.warn("Não foi possível remover evidência após rollback: {}", storageKey, exception);
        }
    }

    private RecursoNaoEncontradoException movimentacaoNaoEncontrada(Long id) {
        return new RecursoNaoEncontradoException(
                "Movimentação com ID " + id + " não encontrada"
        );
    }

    public record ArquivoEvidencia(
            Resource recurso,
            String contentType,
            String nomeArquivo,
            Long tamanhoBytes,
            String sha256
    ) {
    }
}
