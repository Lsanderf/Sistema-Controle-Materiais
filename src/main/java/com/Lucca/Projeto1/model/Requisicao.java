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
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_requisicoes")
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerente_solicitante_id")
    private Usuario gerenteSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encarregado_destinatario_id")
    private Usuario encarregadoDestinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_registrador_id")
    private Usuario operadorRegistrador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encarregado_necessidade_id")
    private Usuario encarregadoNecessidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerente_destinatario_id")
    private Usuario gerenteDestinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigemRequisicao origem = OrigemRequisicao.MANUAL;

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

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizada_por_id")
    private Usuario atualizadaPor;

    @OneToMany(mappedBy = "requisicao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequisicaoItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public Usuario getGerenteSolicitante() { return gerenteSolicitante; }
    public void setGerenteSolicitante(Usuario gerenteSolicitante) { this.gerenteSolicitante = gerenteSolicitante; }
    public Usuario getEncarregadoDestinatario() { return encarregadoDestinatario; }
    public void setEncarregadoDestinatario(Usuario encarregadoDestinatario) { this.encarregadoDestinatario = encarregadoDestinatario; }
    public Usuario getOperadorRegistrador() { return operadorRegistrador; }
    public void setOperadorRegistrador(Usuario operadorRegistrador) { this.operadorRegistrador = operadorRegistrador; }
    public Usuario getEncarregadoNecessidade() { return encarregadoNecessidade; }
    public void setEncarregadoNecessidade(Usuario encarregadoNecessidade) { this.encarregadoNecessidade = encarregadoNecessidade; }
    public Usuario getGerenteDestinatario() { return gerenteDestinatario; }
    public void setGerenteDestinatario(Usuario gerenteDestinatario) { this.gerenteDestinatario = gerenteDestinatario; }
    public OrigemRequisicao getOrigem() { return origem; }
    public void setOrigem(OrigemRequisicao origem) { this.origem = origem; }
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
    public Long getVersao() { return versao; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(LocalDateTime atualizadaEm) { this.atualizadaEm = atualizadaEm; }
    public Usuario getAtualizadaPor() { return atualizadaPor; }
    public void setAtualizadaPor(Usuario atualizadaPor) { this.atualizadaPor = atualizadaPor; }
}
