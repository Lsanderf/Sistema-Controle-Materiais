package com.Lucca.Projeto1.dto.notafiscal;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.model.StatusNotaFiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class NotaFiscalResponse {

    private final Long id;
    private final String numero;
    private final String serie;
    private final String chaveAcesso;
    private final String fornecedor;
    private final String cnpjFornecedor;
    private final LocalDate dataEmissao;
    private final LocalDateTime dataEntrada;
    private final StatusNotaFiscal status;
    private final Long cadastradaPorId;
    private final String cadastradaPorUsername;
    private final LocalDateTime dataCadastro;
    private final List<ItemNotaFiscalResponse> itens;
    private final BigDecimal valorTotal;
    private final List<MovimentacaoResponse> movimentacoes;

    public NotaFiscalResponse(
            Long id,
            String numero,
            String serie,
            String chaveAcesso,
            String fornecedor,
            String cnpjFornecedor,
            LocalDate dataEmissao,
            LocalDateTime dataEntrada,
            StatusNotaFiscal status,
            Long cadastradaPorId,
            String cadastradaPorUsername,
            LocalDateTime dataCadastro,
            List<ItemNotaFiscalResponse> itens,
            BigDecimal valorTotal,
            List<MovimentacaoResponse> movimentacoes
    ) {
        this.id = id;
        this.numero = numero;
        this.serie = serie;
        this.chaveAcesso = chaveAcesso;
        this.fornecedor = fornecedor;
        this.cnpjFornecedor = cnpjFornecedor;
        this.dataEmissao = dataEmissao;
        this.dataEntrada = dataEntrada;
        this.status = status;
        this.cadastradaPorId = cadastradaPorId;
        this.cadastradaPorUsername = cadastradaPorUsername;
        this.dataCadastro = dataCadastro;
        this.itens = itens;
        this.valorTotal = valorTotal;
        this.movimentacoes = movimentacoes;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public String getSerie() {
        return serie;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public String getCnpjFornecedor() {
        return cnpjFornecedor;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public StatusNotaFiscal getStatus() {
        return status;
    }

    public Long getCadastradaPorId() {
        return cadastradaPorId;
    }

    public String getCadastradaPorUsername() {
        return cadastradaPorUsername;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public List<ItemNotaFiscalResponse> getItens() {
        return itens;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public List<MovimentacaoResponse> getMovimentacoes() {
        return movimentacoes;
    }
}
