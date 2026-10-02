package com.Lucca.Projeto1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_solicitacoes_retirada_itens", uniqueConstraints = @UniqueConstraint(
        name = "uk_solicitacoes_retirada_itens_material",
        columnNames = {"solicitacao_retirada_id", "material_id"}
))
public class SolicitacaoRetiradaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitacao_retirada_id", nullable = false)
    private SolicitacaoRetirada solicitacaoRetirada;

    @ManyToOne(optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(nullable = false, updatable = false)
    private Integer quantidade;

    public Long getId() { return id; }
    public SolicitacaoRetirada getSolicitacaoRetirada() { return solicitacaoRetirada; }
    public void setSolicitacaoRetirada(SolicitacaoRetirada solicitacaoRetirada) { this.solicitacaoRetirada = solicitacaoRetirada; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
