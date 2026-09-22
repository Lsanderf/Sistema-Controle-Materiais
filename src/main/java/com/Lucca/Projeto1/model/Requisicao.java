package com.Lucca.Projeto1.model;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_requisicoes")
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gerente_id", nullable = false, updatable = false)
    private Usuario gerente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encarregado_id", nullable = false, updatable = false)
    private Usuario encarregado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false, updatable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusRequisicao status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmado_por_id")
    private Usuario confirmadoPor;

    @Version
    @Column(nullable = false)
    private Long versao;

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Usuario getGerente() { return gerente; }
    public void setGerente(Usuario gerente) { this.gerente = gerente; }
    public Usuario getEncarregado() { return encarregado; }
    public void setEncarregado(Usuario encarregado) { this.encarregado = encarregado; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public StatusRequisicao getStatus() { return status; }
    public void setStatus(StatusRequisicao status) { this.status = status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(LocalDateTime confirmadoEm) { this.confirmadoEm = confirmadoEm; }
    public Usuario getConfirmadoPor() { return confirmadoPor; }
    public void setConfirmadoPor(Usuario confirmadoPor) { this.confirmadoPor = confirmadoPor; }
    public Long getVersao() { return versao; }
}
