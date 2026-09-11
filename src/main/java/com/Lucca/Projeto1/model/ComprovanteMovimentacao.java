package com.Lucca.Projeto1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "tb_comprovantes_movimentacao")
public class ComprovanteMovimentacao {

    @Id
    @Column(name = "movimentacao_id", updatable = false)
    private Long movimentacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoMovimentacao tipo;

    @Column(nullable = false, updatable = false)
    private Integer quantidade;

    @Column(name = "data_movimentacao", nullable = false, updatable = false)
    private LocalDateTime dataMovimentacao;

    @Column(name = "data_finalizacao", nullable = false, updatable = false)
    private LocalDateTime dataFinalizacao;

    @Column(length = 1000, updatable = false)
    private String observacao;

    @Column(name = "material_id", nullable = false, updatable = false)
    private Long materialId;

    @Column(name = "material_nome", nullable = false, length = 150, updatable = false)
    private String materialNome;

    @Column(name = "material_descricao", length = 500, updatable = false)
    private String materialDescricao;

    @Column(name = "funcionario_id", updatable = false)
    private Long funcionarioId;

    @Column(name = "funcionario_nome", length = 150, updatable = false)
    private String funcionarioNome;

    @Column(name = "funcionario_cargo", length = 100, updatable = false)
    private String funcionarioCargo;

    @Column(name = "contrato_id", updatable = false)
    private Long contratoId;

    @Column(name = "contrato_nome", length = 150, updatable = false)
    private String contratoNome;

    @Column(name = "contrato_descricao", length = 500, updatable = false)
    private String contratoDescricao;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(name = "usuario_username", length = 100, updatable = false)
    private String usuarioUsername;

    @Column(name = "nota_fiscal_id", updatable = false)
    private Long notaFiscalId;

    @Column(name = "nota_fiscal_numero", length = 50, updatable = false)
    private String notaFiscalNumero;

    @Column(name = "nota_fiscal_serie", length = 20, updatable = false)
    private String notaFiscalSerie;

    @Column(name = "nota_fiscal_chave_acesso", length = 44, updatable = false)
    private String notaFiscalChaveAcesso;

    @Column(name = "nota_fiscal_fornecedor", length = 200, updatable = false)
    private String notaFiscalFornecedor;

    @Column(name = "nota_fiscal_cnpj_fornecedor", length = 14, updatable = false)
    private String notaFiscalCnpjFornecedor;

    @Column(name = "nota_fiscal_data_emissao", updatable = false)
    private LocalDate notaFiscalDataEmissao;

    @Column(name = "nota_fiscal_data_entrada", updatable = false)
    private LocalDateTime notaFiscalDataEntrada;

    @Column(name = "gerado_em", nullable = false, updatable = false)
    private LocalDateTime geradoEm;

    @Column(nullable = false, updatable = false)
    private Integer versao;

    protected ComprovanteMovimentacao() {
    }

    private ComprovanteMovimentacao(Movimentacao movimentacao) {
        movimentacaoId = movimentacao.getId();
        tipo = movimentacao.getTipo();
        quantidade = movimentacao.getQuantidade();
        dataMovimentacao = movimentacao.getDataMovimentacao();
        dataFinalizacao = movimentacao.getDataFinalizacao();
        observacao = movimentacao.getObservacao();

        Material material = movimentacao.getMaterial();
        materialId = material.getId();
        materialNome = material.getNome();
        materialDescricao = material.getDescricao();

        Funcionario funcionario = movimentacao.getFuncionario();
        if (funcionario != null) {
            funcionarioId = funcionario.getId();
            funcionarioNome = funcionario.getNome();
            funcionarioCargo = funcionario.getCargo();
        }

        Contrato contrato = movimentacao.getContrato();
        if (contrato != null) {
            contratoId = contrato.getId();
            contratoNome = contrato.getNome();
            contratoDescricao = contrato.getDescricao();
        }

        Usuario usuario = movimentacao.getRegistradoPor();
        if (usuario != null) {
            usuarioId = usuario.getId();
            usuarioUsername = usuario.getUsername();
        }

        NotaFiscalEntrada notaFiscal = movimentacao.getNotaFiscal();
        if (notaFiscal != null) {
            notaFiscalId = notaFiscal.getId();
            notaFiscalNumero = notaFiscal.getNumero();
            notaFiscalSerie = notaFiscal.getSerie();
            notaFiscalChaveAcesso = notaFiscal.getChaveAcesso();
            notaFiscalFornecedor = notaFiscal.getFornecedor();
            notaFiscalCnpjFornecedor = notaFiscal.getCnpjFornecedor();
            notaFiscalDataEmissao = notaFiscal.getDataEmissao();
            notaFiscalDataEntrada = notaFiscal.getDataEntrada();
        }

        geradoEm = LocalDateTime.now();
        versao = 1;
    }

    public static ComprovanteMovimentacao registrar(Movimentacao movimentacao) {
        return new ComprovanteMovimentacao(movimentacao);
    }

    public Long getMovimentacaoId() {
        return movimentacaoId;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public LocalDateTime getDataMovimentacao() {
        return dataMovimentacao;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public String getMaterialNome() {
        return materialNome;
    }

    public String getMaterialDescricao() {
        return materialDescricao;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public String getFuncionarioNome() {
        return funcionarioNome;
    }

    public String getFuncionarioCargo() {
        return funcionarioCargo;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public String getContratoNome() {
        return contratoNome;
    }

    public String getContratoDescricao() {
        return contratoDescricao;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioUsername() {
        return usuarioUsername;
    }

    public Long getNotaFiscalId() {
        return notaFiscalId;
    }

    public String getNotaFiscalNumero() {
        return notaFiscalNumero;
    }

    public String getNotaFiscalSerie() {
        return notaFiscalSerie;
    }

    public String getNotaFiscalChaveAcesso() {
        return notaFiscalChaveAcesso;
    }

    public String getNotaFiscalFornecedor() {
        return notaFiscalFornecedor;
    }

    public String getNotaFiscalCnpjFornecedor() {
        return notaFiscalCnpjFornecedor;
    }

    public LocalDate getNotaFiscalDataEmissao() {
        return notaFiscalDataEmissao;
    }

    public LocalDateTime getNotaFiscalDataEntrada() {
        return notaFiscalDataEntrada;
    }

    public LocalDateTime getGeradoEm() {
        return geradoEm;
    }

    public Integer getVersao() {
        return versao;
    }
}
