package com.Lucca.Projeto1.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_requisicao_itens")
public class RequisicaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false)
    private Requisicao requisicao;

    @Column(nullable = false, length = 255)
    private String descricao;

    private Integer quantidade;

    public Long getId() { return id; }
    public Requisicao getRequisicao() { return requisicao; }
    public void setRequisicao(Requisicao requisicao) { this.requisicao = requisicao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
