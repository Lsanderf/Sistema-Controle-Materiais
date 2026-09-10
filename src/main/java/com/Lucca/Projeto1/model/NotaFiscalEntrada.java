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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_notas_fiscais")
public class NotaFiscalEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(nullable = false, length = 20)
    private String serie;

    @Column(name = "chave_acesso", nullable = false, unique = true, length = 44)
    private String chaveAcesso;

    @Column(nullable = false, length = 200)
    private String fornecedor;

    @Column(name = "cnpj_fornecedor", nullable = false, length = 14)
    private String cnpjFornecedor;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_entrada")
    private LocalDateTime dataEntrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotaFiscal status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cadastrada_por_id", nullable = false)
    private Usuario cadastradaPor;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @OneToMany(
            mappedBy = "notaFiscal",
            fetch = FetchType.LAZY,
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE,
                    CascadeType.REMOVE
            },
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<ItemNotaFiscal> itens = new ArrayList<>();

    public NotaFiscalEntrada() {
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getCnpjFornecedor() {
        return cnpjFornecedor;
    }

    public void setCnpjFornecedor(String cnpjFornecedor) {
        this.cnpjFornecedor = cnpjFornecedor;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public StatusNotaFiscal getStatus() {
        return status;
    }

    public void setStatus(StatusNotaFiscal status) {
        this.status = status;
    }

    public Usuario getCadastradaPor() {
        return cadastradaPor;
    }

    public void setCadastradaPor(Usuario cadastradaPor) {
        this.cadastradaPor = cadastradaPor;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public List<ItemNotaFiscal> getItens() {
        return itens;
    }

    public void adicionarItem(ItemNotaFiscal item) {
        item.setNotaFiscal(this);
        itens.add(item);
    }

    public void removerTodosOsItens() {
        itens.clear();
    }
}
