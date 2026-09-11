package com.Lucca.Projeto1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(
        name = "tb_evidencias_movimentacao",
        indexes = @Index(
                name = "idx_evidencias_movimentacao",
                columnList = "movimentacao_id"
        ),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evidencias_movimentacao_tipo",
                columnNames = {"movimentacao_id", "tipo"}
        )
)
public class EvidenciaMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movimentacao_id", nullable = false, updatable = false)
    private Long movimentacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TipoEvidenciaMovimentacao tipo;

    @Column(name = "data_evidencia", nullable = false, updatable = false)
    private LocalDateTime dataEvidencia;

    @Column(name = "funcionario_id", nullable = false, updatable = false)
    private Long funcionarioId;

    @Column(name = "funcionario_nome", nullable = false, length = 150, updatable = false)
    private String funcionarioNome;

    @Column(name = "registrada_por_id", nullable = false, updatable = false)
    private Long registradaPorId;

    @Column(name = "registrada_por_username", nullable = false, length = 100, updatable = false)
    private String registradaPorUsername;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500, updatable = false)
    private String storageKey;

    @Column(name = "nome_arquivo_original", nullable = false, length = 255, updatable = false)
    private String nomeArquivoOriginal;

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false, updatable = false)
    private Long tamanhoBytes;

    @Column(name = "sha256", nullable = false, length = 64, updatable = false)
    private String sha256;

    protected EvidenciaMovimentacao() {
    }

    public EvidenciaMovimentacao(
            Long movimentacaoId,
            TipoEvidenciaMovimentacao tipo,
            LocalDateTime dataEvidencia,
            Long funcionarioId,
            String funcionarioNome,
            Long registradaPorId,
            String registradaPorUsername,
            String storageKey,
            String nomeArquivoOriginal,
            String contentType,
            Long tamanhoBytes,
            String sha256
    ) {
        this.movimentacaoId = movimentacaoId;
        this.tipo = tipo;
        this.dataEvidencia = dataEvidencia;
        this.funcionarioId = funcionarioId;
        this.funcionarioNome = funcionarioNome;
        this.registradaPorId = registradaPorId;
        this.registradaPorUsername = registradaPorUsername;
        this.storageKey = storageKey;
        this.nomeArquivoOriginal = nomeArquivoOriginal;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
        this.sha256 = sha256;
    }

    public Long getId() {
        return id;
    }

    public Long getMovimentacaoId() {
        return movimentacaoId;
    }

    public TipoEvidenciaMovimentacao getTipo() {
        return tipo;
    }

    public LocalDateTime getDataEvidencia() {
        return dataEvidencia;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public String getFuncionarioNome() {
        return funcionarioNome;
    }

    public Long getRegistradaPorId() {
        return registradaPorId;
    }

    public String getRegistradaPorUsername() {
        return registradaPorUsername;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public String getSha256() {
        return sha256;
    }
}
