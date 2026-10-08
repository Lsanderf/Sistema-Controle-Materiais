package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RequisicaoEdicaoIntegrationTests {
    private static final String SENHA = "senhaEdicao123";
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private UsuarioService usuarios;
    @Autowired private ContratoRepository contratos;
    @Autowired private MaterialRepository materiais;
    @Autowired private MovimentacaoRepository movimentacoes;

    @Test
    void gerenteAlteraObservacaoPreservandoIdentidadeItensEAuditoriaOriginal() throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        JsonNode depois = resposta(editar(c, c.gerente(), Map.of("versao", 0, "observacao", "Levar para obra A"))
                .andExpect(status().isOk()).andReturn());
        assertEquals("Levar para obra A", depois.get("observacao").asText());
        for (String campo : List.of("id", "gerenteSolicitante", "encarregadoDestinatario", "contrato", "tipo", "origem",
                "status", "criadaEm", "visualizadaEm", "concluidaEm", "itens")) assertEquals(antes.get(campo), depois.get(campo), campo);
        assertFalse(depois.get("atualizadaEm").isNull());
        assertEquals(antes.get("gerenteSolicitante"), depois.get("atualizadaPor"));
        assertEquals(1, depois.get("versao").asLong());
        assertSemEfeitos(c);
    }

    @Test
    void alteraDescricaoEQuantidadeDeUmItemSemApagarOsDemais() throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        long itemId = antes.get("itens").get(0).get("id").asLong();
        JsonNode depois = resposta(editar(c, c.gerente(), Map.of("versao", 0,
                "itensAlterados", Map.of(Long.toString(itemId), Map.of("descricao", "Capacete G", "quantidade", 8))))
                .andExpect(status().isOk()).andReturn());
        assertEquals(2, depois.get("itens").size());
        assertEquals("Capacete G", item(depois, itemId).get("descricao").asText());
        assertEquals(8, item(depois, itemId).get("quantidade").asInt());
        assertEquals(antes.get("itens").get(1), item(depois, antes.get("itens").get(1).get("id").asLong()));
        assertEquals(antes.get("observacao"), depois.get("observacao"));
    }

    @Test
    void adicionarItemPreservaItensOriginaisInclusiveSeusIds() throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        JsonNode depois = resposta(editar(c, c.gerente(), Map.of("versao", 0,
                "novosItens", List.of(Map.of("descricao", "Bota 42", "quantidade", 2))))
                .andExpect(status().isOk()).andReturn());
        assertEquals(3, depois.get("itens").size());
        for (JsonNode original : antes.get("itens")) assertEquals(original, item(depois, original.get("id").asLong()));
        assertEquals("Levar materiais para obra A", depois.get("observacao").asText());
        assertSemEfeitos(c);
    }

    @Test
    void remocaoExplicitaExcluiSomenteOItemEscolhidoSemDeixarOrfao() throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        long removido = antes.get("itens").get(0).get("id").asLong();
        JsonNode depois = resposta(editar(c, c.gerente(), Map.of("versao", 0, "itensRemovidos", List.of(removido)))
                .andExpect(status().isOk()).andReturn());
        assertEquals(1, depois.get("itens").size());
        assertEquals(antes.get("itens").get(1), depois.get("itens").get(0));
        assertEquals(0L, jdbc.queryForObject("select count(*) from tb_requisicao_itens where id = ?", Long.class, removido));
    }

    @Test
    void removerTodosOuReferenciarItemDeOutraRequisicaoRejeitaSemPersistenciaParcial() throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        List<Long> ids = List.of(antes.get("itens").get(0).get("id").asLong(), antes.get("itens").get(1).get("id").asLong());
        editar(c, c.gerente(), Map.of("versao", 0, "observacao", "Não persistir", "itensRemovidos", ids))
                .andExpect(status().isConflict());
        Cenario outra = cenario("RETIRADA");
        long itemAlheio = consultar(outra).get("itens").get(0).get("id").asLong();
        editar(c, c.gerente(), Map.of("versao", 0, "itensRemovidos", List.of(itemAlheio)))
                .andExpect(status().isConflict());
        assertEquals(antes, consultar(c));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ADMIN", "OPERADOR", "ENCARREGADO"})
    void apenasGerenteCriadorPodeEditar(String role) throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        editar(c, token(usuario(Role.valueOf(role))), Map.of("versao", 0, "observacao", "Proibida"))
                .andExpect(status().isForbidden());
        assertEquals(antes, consultar(c));
    }

    @ParameterizedTest
    @ValueSource(strings = {"visualizar", "concluir", "cancelar"})
    void transicaoDepoisDeAbrirFormularioImpedeSalvar(String acao) throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode formularioAberto = consultar(c);
        mvc.perform(patch("/requisicoes/{id}/" + acao, c.id())
                        .header(HttpHeaders.AUTHORIZATION, acao.equals("cancelar") ? c.gerente() : c.encarregado()))
                .andExpect(status().isOk());
        JsonNode aposTransicao = consultar(c);
        editar(c, c.gerente(), Map.of("versao", formularioAberto.path("versao").asLong(), "observacao", "Não salvar"))
                .andExpect(status().isConflict());
        assertEquals(aposTransicao, consultar(c));
        assertFalse(aposTransicao.get("podeAlterar").asBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "status", "encarregadoDestinatarioId", "contratoId", "tipo", "origem",
            "gerenteSolicitante", "operadorRegistrador", "criadaEm", "visualizadaEm", "concluidaEm", "atualizadaPor", "atualizadaEm"})
    void dtoRejeitaCamposProibidos(String campo) throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        Map<String, Object> payload = new HashMap<>(Map.of("versao", 0, "observacao", "Não persistir"));
        payload.put(campo, "proibido");
        editar(c, c.gerente(), payload).andExpect(status().isBadRequest());
        assertEquals(antes, consultar(c));
    }

    @Test
    void dtoTambemRejeitaIdentidadeEEstadoDentroDoConteudoDosItens() throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        editar(c, c.gerente(), Map.of("versao", 0, "novosItens", List.of(
                Map.of("descricao", "Bota", "quantidade", 1, "id", 999, "status", "CONCLUIDA"))))
                .andExpect(status().isBadRequest());
        assertEquals(antes, consultar(c));
    }

    @Test
    void versaoObrigatoriaEObservacaoPodeSerLimpaExplicitamente() throws Exception {
        Cenario c = cenario("RETIRADA");
        editar(c, c.gerente(), Map.of("observacao", "Sem versão")).andExpect(status().isBadRequest());
        Map<String, Object> payload = new HashMap<>(Map.of("versao", 0));
        payload.put("observacao", null);
        editar(c, c.gerente(), payload).andExpect(status().isOk()).andExpect(jsonPath("$.observacao").doesNotExist());
        assertEquals(2, consultar(c).get("itens").size());
    }

    @Test
    void falhaDeVersaoTraduzidaPeloSpringTambemRetorna409() throws Exception {
        var controller = new ConflitoDeVersaoController();
        var standalone = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.Lucca.Projeto1.exception.GlobalExceptionHandler()).build();
        standalone.perform(patch("/requisicao-teste-concorrencia"))
                .andExpect(status().isConflict());
    }

    @org.springframework.web.bind.annotation.RestController
    @org.springframework.boot.test.context.TestComponent
    static class ConflitoDeVersaoController {
        @org.springframework.web.bind.annotation.PatchMapping("/requisicao-teste-concorrencia")
        void alterar() {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException("Requisicao", 42L);
        }
    }

    @Test
    void faltaEstoqueEDevolucaoNaoSaoEditaveis() throws Exception {
        Cenario devolucao = cenario("DEVOLUCAO");
        editar(devolucao, devolucao.gerente(), Map.of("versao", 0, "observacao", "Não alterar"))
                .andExpect(status().isConflict());
        Usuario operador = usuario(Role.OPERADOR);
        Material material = materiais.save(new Material("Falta " + UUID.randomUUID(), "Teste", 2));
        Usuario gerente = usuario(Role.GERENTE);
        Usuario encarregado = usuario(Role.ENCARREGADO);
        Contrato contrato = contratos.save(new Contrato("Compra " + UUID.randomUUID(), "Teste", true));
        JsonNode falta = resposta(mvc.perform(post("/requisicoes/falta-estoque")
                        .header(HttpHeaders.AUTHORIZATION, token(operador)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("gerenteDestinatarioId", gerente.getId(),
                                "encarregadoNecessidadeId", encarregado.getId(), "contratoId", contrato.getId(),
                                "itens", List.of(Map.of("materialId", material.getId(), "quantidadeSolicitada", 5))))))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(patch("/requisicoes/{id}", falta.get("id").asLong()).header(HttpHeaders.AUTHORIZATION, token(gerente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versao\":0,\"observacao\":\"Proibida\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void consultaNaoMudaEstadoEDestinatarioCorretoPodeMarcarVisualizada() throws Exception {
        Cenario c = cenario("RETIRADA");
        assertTrue(consultar(c).path("podeAlterar").asBoolean());
        assertFalse(consultar(c).get("podeMarcarVisualizada").asBoolean());
        JsonNode destinatario = resposta(mvc.perform(get("/requisicoes/{id}", c.id())
                        .header(HttpHeaders.AUTHORIZATION, c.encarregado())).andExpect(status().isOk()).andReturn());
        assertTrue(destinatario.get("podeMarcarVisualizada").asBoolean());
        mvc.perform(patch("/requisicoes/{id}/visualizar", c.id()).header(HttpHeaders.AUTHORIZATION, c.gerente()))
                .andExpect(status().isForbidden());
        JsonNode visualizada = resposta(mvc.perform(patch("/requisicoes/{id}/visualizar", c.id())
                        .header(HttpHeaders.AUTHORIZATION, c.encarregado())).andExpect(status().isOk()).andReturn());
        assertEquals("VISUALIZADA", visualizada.get("status").asText());
        assertFalse(visualizada.get("podeMarcarVisualizada").asBoolean());
        assertEquals(1, visualizada.get("versao").asLong());
    }

    @Test
    void versaoAntigaNaoSobrescreveEdicaoMaisRecente() throws Exception {
        Cenario c = cenario("RETIRADA");
        editar(c, c.gerente(), Map.of("versao", 0, "observacao", "Primeira edição")).andExpect(status().isOk());
        editar(c, c.gerente(), Map.of("versao", 0, "observacao", "Segunda edição antiga")).andExpect(status().isConflict());
        assertEquals("Primeira edição", consultar(c).get("observacao").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"editar", "visualizar", "cancelar", "concluir"})
    void operacoesConcorrentesNaoSobrescrevemConteudoOuEstado(String outraAcao) throws Exception {
        Cenario c = cenario("RETIRADA");
        CountDownLatch iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var edicao = executor.submit(() -> {
                iniciar.await(10, TimeUnit.SECONDS);
                return editar(c, c.gerente(), Map.of("versao", 0, "observacao", "Edição concorrente")).andReturn();
            });
            var outra = executor.submit(() -> {
                iniciar.await(10, TimeUnit.SECONDS);
                if (outraAcao.equals("editar")) return editar(c, c.gerente(), Map.of("versao", 0, "observacao", "Outra edição")).andReturn();
                return mvc.perform(patch("/requisicoes/{id}/" + outraAcao, c.id())
                        .header(HttpHeaders.AUTHORIZATION, outraAcao.equals("cancelar") ? c.gerente() : c.encarregado())).andReturn();
            });
            iniciar.countDown();
            MvcResult rEdicao = edicao.get(25, TimeUnit.SECONDS);
            MvcResult rOutra = outra.get(25, TimeUnit.SECONDS);
            if (outraAcao.equals("editar")) {
                assertEquals(List.of(200, 409), List.of(rEdicao.getResponse().getStatus(), rOutra.getResponse().getStatus()).stream().sorted().toList());
                String vencedora = rEdicao.getResponse().getStatus() == 200 ? "Edição concorrente" : "Outra edição";
                assertEquals(vencedora, consultar(c).get("observacao").asText());
            } else {
                assertEquals(200, rOutra.getResponse().getStatus(), String.valueOf(rOutra.getResolvedException()));
                assertTrue(List.of(200, 409).contains(rEdicao.getResponse().getStatus()));
                String esperado = Map.of("visualizar", "VISUALIZADA", "cancelar", "CANCELADA", "concluir", "CONCLUIDA").get(outraAcao);
                JsonNode finalizada = consultar(c);
                assertEquals(esperado, finalizada.get("status").asText());
                assertEquals(rEdicao.getResponse().getStatus() == 200 ? "Edição concorrente" : "Levar materiais para obra A",
                        finalizada.get("observacao").asText());
            }
        }
        assertSemEfeitos(c);
    }

    @ParameterizedTest
    @ValueSource(strings = {"descricaoVazia", "descricaoLonga", "quantidadeZero", "quantidadeAlta", "observacaoLonga", "limiteItens"})
    void edicaoPreservaLimitesDaCriacao(String caso) throws Exception {
        Cenario c = cenario("RETIRADA");
        JsonNode antes = consultar(c);
        Map<String, Object> payload = new HashMap<>(Map.of("versao", 0));
        switch (caso) {
            case "descricaoVazia" -> payload.put("novosItens", List.of(Map.of("descricao", "  ", "quantidade", 1)));
            case "descricaoLonga" -> payload.put("novosItens", List.of(Map.of("descricao", "x".repeat(256), "quantidade", 1)));
            case "quantidadeZero" -> payload.put("novosItens", List.of(Map.of("descricao", "Bota", "quantidade", 0)));
            case "quantidadeAlta" -> payload.put("novosItens", List.of(Map.of("descricao", "Bota", "quantidade", 10001)));
            case "observacaoLonga" -> payload.put("observacao", "x".repeat(1001));
            case "limiteItens" -> payload.put("novosItens", java.util.Collections.nCopies(99, Map.of("descricao", "Bota", "quantidade", 1)));
        }
        editar(c, c.gerente(), payload).andExpect(caso.equals("limiteItens") ? status().isConflict() : status().isBadRequest());
        assertEquals(antes, consultar(c));
    }

    protected Cenario cenario(String tipo) throws Exception {
        Usuario gerente = usuario(Role.GERENTE);
        Usuario encarregado = usuario(Role.ENCARREGADO);
        Contrato contrato = contratos.save(new Contrato("Edição " + UUID.randomUUID(), "Teste", true));
        String gerenteToken = token(gerente);
        String encarregadoToken = token(encarregado);
        JsonNode criada = resposta(mvc.perform(post("/requisicoes").header(HttpHeaders.AUTHORIZATION, gerenteToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                                "encarregadoDestinatarioId", encarregado.getId(), "contratoId", contrato.getId(), "tipo", tipo,
                                "observacao", "Levar materiais para obra A", "itens", List.of(
                                        Map.of("descricao", "Capacete M", "quantidade", 5), Map.of("descricao", "Luva M", "quantidade", 10))))))
                .andExpect(status().isCreated()).andReturn());
        return new Cenario(criada.get("id").asLong(), gerenteToken, encarregadoToken, materiais.count(), movimentacoes.count());
    }

    protected ResultActions editar(Cenario c, String token, Map<String, ?> payload) throws Exception {
        return mvc.perform(patch("/requisicoes/{id}", c.id()).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)));
    }

    protected JsonNode consultar(Cenario c) throws Exception {
        return resposta(mvc.perform(get("/requisicoes/{id}", c.id()).header(HttpHeaders.AUTHORIZATION, c.gerente()))
                .andExpect(status().isOk()).andReturn());
    }

    protected JsonNode resposta(MvcResult resultado) throws Exception {
        return mapper.readTree(resultado.getResponse().getContentAsString());
    }

    private JsonNode item(JsonNode requisicao, long id) {
        for (JsonNode item : requisicao.get("itens")) if (item.get("id").asLong() == id) return item;
        throw new AssertionError("Item não encontrado: " + id);
    }

    protected void assertSemEfeitos(Cenario c) {
        assertEquals(c.materiais(), materiais.count());
        assertEquals(c.movimentacoes(), movimentacoes.count());
        assertEquals(1L, jdbc.queryForObject("select count(*) from tb_requisicoes where id = ?", Long.class, c.id()));
    }

    private Usuario usuario(Role role) {
        String cpf;
        do { cpf = TestUsuarioFactory.proximoCpf(); }
        while (jdbc.queryForObject("select count(*) from tb_usuarios where cpf = ?", Long.class, cpf) > 0);
        String username = role.name().toLowerCase() + "-ed-" + UUID.randomUUID().toString().substring(0, 8);
        return usuarios.criarUsuario("Usuário " + username, cpf, "11999999999", username, SENHA, role, true);
    }

    private String token(Usuario usuario) throws Exception {
        JsonNode login = resposta(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", usuario.getUsername(), "password", SENHA))))
                .andExpect(status().isOk()).andReturn());
        return "Bearer " + login.get("token").asText();
    }

    protected record Cenario(long id, String gerente, String encarregado, long materiais, long movimentacoes) { }
}
