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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final TipoEvidenciaMovimentacao ASSINATURA =
            TipoEvidenciaMovimentacao.ASSINATURA;

    private final MovimentacaoRepository movimentacaoRepository;
    private final EvidenciaMovimentacaoRepository evidenciaRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final EvidenciaStorage storage;
    private final long tamanhoMaximoBytes;

    public EvidenciaMovimentacaoService(
            MovimentacaoRepository movimentacaoRepository,
            EvidenciaMovimentacaoRepository evidenciaRepository,
            UsuarioAutenticadoService usuarioAutenticadoService,
            EvidenciaStorage storage,
            @Value("${app.evidencias.tamanho-maximo:2MB}") String tamanhoMaximo
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.storage = storage;
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

        byte[] conteudo = lerConteudo(arquivo);
        FormatoImagem formato = identificarFormato(conteudo);
        Usuario usuario = usuarioAutenticadoService.obter();
        Funcionario funcionario = movimentacao.getFuncionario();
        String storageKey = criarStorageKey(movimentacaoId, formato.extensao());

        storage.armazenar(storageKey, conteudo);
        registrarLimpezaEmCasoDeRollback(storageKey);

        try {
            EvidenciaMovimentacao evidencia = new EvidenciaMovimentacao(
                    movimentacaoId,
                    ASSINATURA,
                    LocalDateTime.now(),
                    funcionario.getId(),
                    funcionario.getNome(),
                    usuario.getId(),
                    usuario.getUsername(),
                    storageKey,
                    normalizarNomeArquivo(
                            arquivo.getOriginalFilename(),
                            formato.extensao()
                    ),
                    formato.contentType(),
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

    private byte[] lerConteudo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException(
                    "O arquivo da assinatura é obrigatório"
            );
        }
        if (arquivo.getSize() > tamanhoMaximoBytes) {
            throw new RegraNegocioException(
                    "O arquivo da assinatura excede o tamanho máximo permitido"
            );
        }

        try {
            byte[] conteudo = arquivo.getBytes();
            if (conteudo.length > tamanhoMaximoBytes) {
                throw new RegraNegocioException(
                        "O arquivo da assinatura excede o tamanho máximo permitido"
                );
            }
            return conteudo;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível ler o arquivo da assinatura",
                    exception
            );
        }
    }

    private FormatoImagem identificarFormato(byte[] conteudo) {
        if (conteudo.length >= 8
                && (conteudo[0] & 0xff) == 0x89
                && conteudo[1] == 0x50
                && conteudo[2] == 0x4e
                && conteudo[3] == 0x47
                && conteudo[4] == 0x0d
                && conteudo[5] == 0x0a
                && conteudo[6] == 0x1a
                && conteudo[7] == 0x0a) {
            return new FormatoImagem("image/png", "png");
        }

        if (conteudo.length >= 3
                && (conteudo[0] & 0xff) == 0xff
                && (conteudo[1] & 0xff) == 0xd8
                && (conteudo[2] & 0xff) == 0xff) {
            return new FormatoImagem("image/jpeg", "jpg");
        }

        throw new RegraNegocioException(
                "A assinatura deve ser uma imagem PNG ou JPEG válida"
        );
    }

    private String criarStorageKey(Long movimentacaoId, String extensao) {
        return "movimentacoes/" + movimentacaoId
                + "/assinatura/" + UUID.randomUUID() + "." + extensao;
    }

    private String normalizarNomeArquivo(String nomeOriginal, String extensao) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "assinatura." + extensao;
        }

        String caminhoNormalizado = nomeOriginal.replace('\\', '/');
        String nome = caminhoNormalizado
                .substring(caminhoNormalizado.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();

        if (nome.isBlank()) {
            return "assinatura." + extensao;
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
                        if (status != STATUS_COMMITTED) {
                            removerSilenciosamente(storageKey);
                        }
                    }
                }
        );
    }

    private void removerSilenciosamente(String storageKey) {
        try {
            storage.remover(storageKey);
        } catch (RuntimeException ignored) {
            // A exceção transacional original não deve ser ocultada.
        }
    }

    private RecursoNaoEncontradoException movimentacaoNaoEncontrada(Long id) {
        return new RecursoNaoEncontradoException(
                "Movimentação com ID " + id + " não encontrada"
        );
    }

    private record FormatoImagem(String contentType, String extensao) {
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
