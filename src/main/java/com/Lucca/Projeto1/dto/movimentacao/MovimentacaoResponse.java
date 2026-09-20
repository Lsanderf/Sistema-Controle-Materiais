package com.Lucca.Projeto1.dto.movimentacao;

import com.Lucca.Projeto1.model.TipoMovimentacao;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;


    public class MovimentacaoResponse {

        private Long id;
        private String funcionario;
        private String contrato;
        private String material;
        private Integer quantidade;
        private TipoMovimentacao tipo;
        private Long usuarioId;
        private String usuarioUsername;
        private Long notaFiscalId;
        private String observacao;
        private Long movimentacaoOrigemId;
        private boolean estornada;
        private Long estornoId;

        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        private LocalDateTime dataMovimentacao;

        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        private LocalDateTime dataFinalizacao;

        public MovimentacaoResponse() {
        }

        public MovimentacaoResponse(
                Long id,
                String funcionario,
                String contrato,
                String material,
                Integer quantidade,
                TipoMovimentacao tipo,
                LocalDateTime dataMovimentacao,
                LocalDateTime dataFinalizacao,
                Long usuarioId,
                String usuarioUsername,
                Long notaFiscalId,
                String observacao,
                Long movimentacaoOrigemId,
                Long estornoId
        ) {
            this.id = id;
            this.funcionario = funcionario;
            this.contrato = contrato;
            this.material = material;
            this.quantidade = quantidade;
            this.tipo = tipo;
            this.dataMovimentacao = dataMovimentacao;
            this.dataFinalizacao = dataFinalizacao;
            this.usuarioId = usuarioId;
            this.usuarioUsername = usuarioUsername;
            this.notaFiscalId = notaFiscalId;
            this.observacao = observacao;
            this.movimentacaoOrigemId = movimentacaoOrigemId;
            this.estornoId = estornoId;
            this.estornada = estornoId != null;
        }

        public Long getId() {
            return id;
        }

        public String getFuncionario() {
            return funcionario;
        }

        public String getContrato() {
            return contrato;
        }

        public String getMaterial() {
            return material;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public TipoMovimentacao getTipo() {
            return tipo;
        }

        public LocalDateTime getDataMovimentacao() {
            return dataMovimentacao;
        }

        public LocalDateTime getDataFinalizacao() {
            return dataFinalizacao;
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

        public String getObservacao() {
            return observacao;
        }

        public Long getMovimentacaoOrigemId() {
            return movimentacaoOrigemId;
        }

        public boolean isEstornada() {
            return estornada;
        }

        public Long getEstornoId() {
            return estornoId;
        }
    }

