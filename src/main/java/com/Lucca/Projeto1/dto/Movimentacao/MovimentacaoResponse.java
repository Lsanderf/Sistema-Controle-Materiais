package com.Lucca.Projeto1.dto.Movimentacao;

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

        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        private LocalDateTime dataMovimentacao;

        public MovimentacaoResponse() {
        }

        public MovimentacaoResponse(
                Long id,
                String funcionario,
                String contrato,
                String material,
                Integer quantidade,
                TipoMovimentacao tipo,
                LocalDateTime dataMovimentacao
        ) {
            this.id = id;
            this.funcionario = funcionario;
            this.contrato = contrato;
            this.material = material;
            this.quantidade = quantidade;
            this.tipo = tipo;
            this.dataMovimentacao = dataMovimentacao;
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
    }

