package com.Lucca.Projeto1.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(
        name = "tb_movimentacoes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movimentacao_usuario_idempotency",
                        columnNames = {
                                "usuario_id",
                                "idempotency_key"
                        }
                )
        }
)
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "funcionario_id", nullable = true, updatable = false)
    private Funcionario funcionario;


    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contrato_id", nullable = true, updatable = false)
    private Contrato contrato;

    @ManyToOne(optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(nullable = false, updatable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoMovimentacao tipo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataMovimentacao;

    @Column(name = "data_finalizacao", nullable = false, updatable = false)
    private LocalDateTime dataFinalizacao;

    @Column(length = 1000, updatable = false)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_id", nullable = true, updatable = false)
    private Usuario registradoPor;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "nota_fiscal_id", nullable = true, updatable = false)
    private NotaFiscalEntrada notaFiscal;

    @Column(
            name = "idempotency_key",
            length = 100,
            updatable = false
    )
    private String idempotencyKey;

    @Column(
            name = "request_fingerprint",
            length = 64,
            updatable = false
    )
    private String requestFingerprint;

    public Movimentacao() {
    }

    @PrePersist
    private void preencherDatasDeConclusao() {
        if (dataMovimentacao == null) {
            dataMovimentacao = LocalDateTime.now();
        }
        if (dataFinalizacao == null) {
            dataFinalizacao = dataMovimentacao;
        }
    }

    public Long getId() {
        return id;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getDataMovimentacao() {
        return dataMovimentacao;
    }

    public void setDataMovimentacao(LocalDateTime dataMovimentacao) {
        this.dataMovimentacao = dataMovimentacao;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }

    public void setDataFinalizacao(LocalDateTime dataFinalizacao) {
        this.dataFinalizacao = dataFinalizacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    public NotaFiscalEntrada getNotaFiscal() {
        return notaFiscal;
    }

    public void setNotaFiscal(NotaFiscalEntrada notaFiscal) {
        this.notaFiscal = notaFiscal;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }
}
