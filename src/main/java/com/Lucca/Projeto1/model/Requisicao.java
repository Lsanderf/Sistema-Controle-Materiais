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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_requisicoes")
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gerente_solicitante_id", nullable = false)
    private Usuario gerenteSolicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encarregado_destinatario_id", nullable = false)
    private Usuario encarregadoDestinatario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @Column(length = 1000)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRequisicao status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "visualizada_em")
    private LocalDateTime visualizadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @OneToMany(mappedBy = "requisicao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequisicaoItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public Usuario getGerenteSolicitante() { return gerenteSolicitante; }
    public void setGerenteSolicitante(Usuario gerenteSolicitante) { this.gerenteSolicitante = gerenteSolicitante; }
    public Usuario getEncarregadoDestinatario() { return encarregadoDestinatario; }
    public void setEncarregadoDestinatario(Usuario encarregadoDestinatario) { this.encarregadoDestinatario = encarregadoDestinatario; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public StatusRequisicao getStatus() { return status; }
    public void setStatus(StatusRequisicao status) { this.status = status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }
    public LocalDateTime getVisualizadaEm() { return visualizadaEm; }
    public void setVisualizadaEm(LocalDateTime visualizadaEm) { this.visualizadaEm = visualizadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }
    public List<RequisicaoItem> getItens() { return itens; }
    public void adicionarItem(RequisicaoItem item) { item.setRequisicao(this); itens.add(item); }
}
