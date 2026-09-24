package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.RequisicaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RequisicaoIntegrationTests {

    private static final String SENHA = "senhaRequisicao123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RequisicaoRepository requisicaoRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private MaterialRepository materialRepository;

    private String gerenteToken;
    private String outroGerenteToken;
    private String encarregadoToken;
    private String outroEncarregadoToken;
    private Contrato contrato;
    private Long encarregadoId;
    private Long outroEncarregadoId;

    @BeforeEach
    void preparar() throws Exception {
        requisicaoRepository.deleteAll();
        contratoRepository.deleteAll();
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(usuarioService, "gerente-req", SENHA, Role.GERENTE, true);
        TestUsuarioFactory.criarUsuario(usuarioService, "outro-gerente-req", SENHA, Role.GERENTE, true);
        encarregadoId = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-req", SENHA, Role.ENCARREGADO, true).getId();
        outroEncarregadoId = TestUsuarioFactory.criarUsuario(usuarioService, "outro-encarregado-req", SENHA, Role.ENCARREGADO, true).getId();
        gerenteToken = token("gerente-req");
        outroGerenteToken = token("outro-gerente-req");
        encarregadoToken = token("encarregado-req");
        outroEncarregadoToken = token("outro-encarregado-req");
        contrato = contratoRepository.save(new Contrato("Contrato requisição", "Contrato de teste", true));
    }

    @Test
    void gerenteCriaRequisicaoTextualESemMovimentarEstoque() throws Exception {
        long id = criarRequisicao(gerenteToken, encarregadoId);
        mockMvc.perform(get("/requisicoes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.itens.length()").value(3))
                .andExpect(jsonPath("$.itens[0].descricao").value("Parafuso"))
                .andExpect(jsonPath("$.itens[1].descricao").value("Capacete"))
                .andExpect(jsonPath("$.itens[2].descricao").value("Luvas M"));
        mockMvc.perform(post("/movimentacoes").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
        assertEquals(0, materialRepository.count());
    }

    @Test
    void gerenteVeSomenteAsPropriasRequisicoesENaoVeDeOutroGerente() throws Exception {
        long propria = criarRequisicao(gerenteToken, encarregadoId);
        long alheia = criarRequisicao(outroGerenteToken, encarregadoId);
        mockMvc.perform(get("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propria));
        mockMvc.perform(get("/requisicoes/{id}", alheia).header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void encarregadoVisualizaEConcluiApenasRequisicaoDestinadaAEleSemAlterarEstoque() throws Exception {
        long destinada = criarRequisicao(gerenteToken, encarregadoId);
        long deOutro = criarRequisicao(gerenteToken, outroEncarregadoId);
        mockMvc.perform(get("/requisicoes/{id}", destinada).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(destinada))
                .andExpect(jsonPath("$.itens[0].descricao").value("Parafuso"));
        mockMvc.perform(get("/requisicoes/{id}", deOutro).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(patch("/requisicoes/{id}/visualizar", destinada).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VISUALIZADA"));
        mockMvc.perform(patch("/requisicoes/{id}/concluir", destinada).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        assertEquals(0, materialRepository.count());
        mockMvc.perform(patch("/requisicoes/{id}/visualizar", destinada).header(HttpHeaders.AUTHORIZATION, bearer(outroEncarregadoToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requisicoesExigemAutenticacao() throws Exception {
        mockMvc.perform(get("/requisicoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/requisicoes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requisicaoRejeitaDescricaoVaziaEQuantidadeInvalida() throws Exception {
        mockMvc.perform(post("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("encarregadoDestinatarioId", encarregadoId, "contratoId", contrato.getId(), "tipo", "RETIRADA",
                                "itens", List.of(Map.of("descricao", "   ", "quantidade", 1))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("encarregadoDestinatarioId", encarregadoId, "contratoId", contrato.getId(), "tipo", "RETIRADA",
                                "itens", List.of(Map.of("descricao", "Parafuso", "quantidade", 0))))))
                .andExpect(status().isBadRequest());
    }

    private long criarRequisicao(String token, Long destinatarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/requisicoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoDestinatarioId", destinatarioId,
                                "contratoId", contrato.getId(),
                                "tipo", "RETIRADA",
                                "itens", List.of(
                                        Map.of("descricao", " Parafuso ", "quantidade", 10),
                                        Map.of("descricao", "Capacete", "quantidade", 5),
                                        Map.of("descricao", "Luvas M", "quantidade", 5)
                                )
                        ))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", SENHA))))
                .andExpect(status().isOk()).andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
    private String bearer(String token) { return "Bearer " + token; }
}
