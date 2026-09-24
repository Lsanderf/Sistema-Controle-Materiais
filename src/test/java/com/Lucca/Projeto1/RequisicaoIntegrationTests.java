package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
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
    private Material material;
    private Long encarregadoId;
    private Long outroEncarregadoId;

    @BeforeEach
    void preparar() throws Exception {
        requisicaoRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
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
        material = materialRepository.save(new Material("Material requisição", "Material de teste", 41));
    }

    @Test
    void gerenteCriaRequisicaoEContinuaProibidoDeMovimentarEstoque() throws Exception {
        long id = criarRequisicao(gerenteToken, encarregadoId, 3);
        mockMvc.perform(get("/requisicoes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"));
        mockMvc.perform(post("/movimentacoes").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
        assertEquals(41, estoque());
    }

    @Test
    void gerenteVeSomenteAsPropriasRequisicoesENaoVeDeOutroGerente() throws Exception {
        long propria = criarRequisicao(gerenteToken, encarregadoId, 3);
        long alheia = criarRequisicao(outroGerenteToken, encarregadoId, 4);
        mockMvc.perform(get("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propria));
        mockMvc.perform(get("/requisicoes/{id}", alheia).header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void encarregadoVisualizaEConcluiApenasRequisicaoDestinadaAEleSemAlterarEstoque() throws Exception {
        long destinada = criarRequisicao(gerenteToken, encarregadoId, 3);
        long deOutro = criarRequisicao(gerenteToken, outroEncarregadoId, 4);
        mockMvc.perform(get("/requisicoes/{id}", destinada).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(destinada));
        mockMvc.perform(get("/requisicoes/{id}", deOutro).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(patch("/requisicoes/{id}/visualizar", destinada).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VISUALIZADA"));
        assertEquals(41, estoque());
        mockMvc.perform(patch("/requisicoes/{id}/concluir", destinada).header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        assertEquals(41, estoque());
        mockMvc.perform(patch("/requisicoes/{id}/visualizar", destinada).header(HttpHeaders.AUTHORIZATION, bearer(outroEncarregadoToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requisicoesExigemAutenticacao() throws Exception {
        mockMvc.perform(get("/requisicoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/requisicoes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private long criarRequisicao(String token, Long destinatarioId, int quantidade) throws Exception {
        MvcResult result = mockMvc.perform(post("/requisicoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoDestinatarioId", destinatarioId,
                                "contratoId", contrato.getId(),
                                "tipo", "RETIRADA",
                                "itens", List.of(Map.of("materialId", material.getId(), "quantidade", quantidade))
                        ))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private int estoque() { return materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque(); }
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
