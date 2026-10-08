package com.Lucca.Projeto1.dto.requisicao;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Alterações de conteúdo; os IDs nas coleções apenas identificam itens já pertencentes ao agregado. */
public class RequisicaoEdicaoRequest {
    @NotNull @PositiveOrZero
    private Long versao;

    @Size(max = 1000, message = "A observação deve ter no máximo 1.000 caracteres")
    private String observacao;
    private boolean observacaoInformada;

    @NotNull @Size(max = 100)
    private Map<@Positive Long, @NotNull @Valid Item> itensAlterados = Map.of();

    @NotNull @Size(max = 100)
    private List<@NotNull @Valid Item> novosItens = List.of();

    @NotNull @Size(max = 100)
    private List<@NotNull @Positive Long> itensRemovidos = List.of();

    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) {
        this.observacao = observacao;
        this.observacaoInformada = true;
    }
    public boolean observacaoFoiInformada() { return observacaoInformada; }
    public Map<Long, Item> getItensAlterados() { return itensAlterados; }
    public void setItensAlterados(Map<Long, Item> itensAlterados) { this.itensAlterados = itensAlterados; }
    public List<Item> getNovosItens() { return novosItens; }
    public void setNovosItens(List<Item> novosItens) { this.novosItens = novosItens; }
    public List<Long> getItensRemovidos() { return itensRemovidos; }
    public void setItensRemovidos(List<Long> itensRemovidos) { this.itensRemovidos = itensRemovidos; }

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo não permitido: " + campo);
    }

    public record Item(String descricao, Integer quantidade) {
        // Reutiliza as validações da criação sem mudar a desserialização daquele fluxo.
        @Valid @JsonIgnore
        public RequisicaoItemRequest getConteudo() {
            return new RequisicaoItemRequest(descricao, quantidade);
        }

        @JsonAnySetter
        public void rejeitarCampoDesconhecido(String campo, Object valor) {
            throw new IllegalArgumentException("Campo não permitido no item: " + campo);
        }
    }
}
