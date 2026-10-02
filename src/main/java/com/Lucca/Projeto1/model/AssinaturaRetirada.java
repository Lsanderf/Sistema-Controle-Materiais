package com.Lucca.Projeto1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_assinaturas_retirada")
public class AssinaturaRetirada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_retirada_id", nullable = false, unique = true)
    private SolicitacaoRetirada solicitacaoRetirada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encarregado_assinante_id", nullable = false, updatable = false)
    private Usuario encarregadoAssinante;

    @Column(name = "data_assinatura", nullable = false, updatable = false)
    private LocalDateTime dataAssinatura;

    @Column(name = "storage_key", nullable = false, length = 500, updatable = false)
    private String storageKey;

    @Column(name = "nome_arquivo_original", nullable = false, length = 255, updatable = false)
    private String nomeArquivoOriginal;

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false, updatable = false)
    private Long tamanhoBytes;

    @Column(nullable = false, length = 64, updatable = false)
    private String sha256;

    public Long getId() { return id; }
    public SolicitacaoRetirada getSolicitacaoRetirada() { return solicitacaoRetirada; }
    public void setSolicitacaoRetirada(SolicitacaoRetirada solicitacaoRetirada) { this.solicitacaoRetirada = solicitacaoRetirada; }
    public Usuario getEncarregadoAssinante() { return encarregadoAssinante; }
    public void setEncarregadoAssinante(Usuario encarregadoAssinante) { this.encarregadoAssinante = encarregadoAssinante; }
    public LocalDateTime getDataAssinatura() { return dataAssinatura; }
    public void setDataAssinatura(LocalDateTime dataAssinatura) { this.dataAssinatura = dataAssinatura; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getNomeArquivoOriginal() { return nomeArquivoOriginal; }
    public void setNomeArquivoOriginal(String nomeArquivoOriginal) { this.nomeArquivoOriginal = nomeArquivoOriginal; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
}
