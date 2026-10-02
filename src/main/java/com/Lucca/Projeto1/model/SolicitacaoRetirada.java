package com.Lucca.Projeto1.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_solicitacoes_retirada", uniqueConstraints = @UniqueConstraint(
        name = "uk_solicitacoes_retirada_operador_idempotency",
        columnNames = {"operador_responsavel_id", "idempotency_key"}
))
public class SolicitacaoRetirada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_responsavel_id", nullable = false, updatable = false)
    private Usuario operadorResponsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encarregado_assinante_id", nullable = false, updatable = false)
    private Usuario encarregadoAssinante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", updatable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSolicitacaoRetirada status;

    @Column(length = 1000, updatable = false)
    private String observacao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(name = "confirmada_em")
    private LocalDateTime confirmadaEm;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64, updatable = false)
    private String requestFingerprint;

    @OneToMany(mappedBy = "solicitacaoRetirada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitacaoRetiradaItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public Usuario getOperadorResponsavel() { return operadorResponsavel; }
    public void setOperadorResponsavel(Usuario operadorResponsavel) { this.operadorResponsavel = operadorResponsavel; }
    public Usuario getEncarregadoAssinante() { return encarregadoAssinante; }
    public void setEncarregadoAssinante(Usuario encarregadoAssinante) { this.encarregadoAssinante = encarregadoAssinante; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public StatusSolicitacaoRetirada getStatus() { return status; }
    public void setStatus(StatusSolicitacaoRetirada status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }
    public LocalDateTime getConfirmadaEm() { return confirmadaEm; }
    public void setConfirmadaEm(LocalDateTime confirmadaEm) { this.confirmadaEm = confirmadaEm; }
    public LocalDateTime getCanceladaEm() { return canceladaEm; }
    public void setCanceladaEm(LocalDateTime canceladaEm) { this.canceladaEm = canceladaEm; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
    public List<SolicitacaoRetiradaItem> getItens() { return itens; }
    public void adicionarItem(SolicitacaoRetiradaItem item) { item.setSolicitacaoRetirada(this); itens.add(item); }
}
