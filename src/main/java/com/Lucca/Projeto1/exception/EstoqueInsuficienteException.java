package com.Lucca.Projeto1.exception;

import java.util.List;

public class EstoqueInsuficienteException extends RuntimeException {
    private final List<Item> itens;

    public EstoqueInsuficienteException(List<Item> itens) {
        super("Estoque insuficiente");
        this.itens = List.copyOf(itens);
    }

    public List<Item> getItens() { return itens; }

    public record Item(Long materialId, String material, Integer disponivel, Integer solicitado, Integer faltante) { }
}
