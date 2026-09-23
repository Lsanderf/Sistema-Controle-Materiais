package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.ContratoRepository;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MatrizAutorizacaoIntegrationTests {

    private static final String SENHA = "senhaForte123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    private String adminToken;
    private String operadorToken;
    private String gerenteToken;
    private String encarregadoToken;

    @BeforeEach
    void prepararUsuarios() throws Exception {
        contratoRepository.deleteAll();
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(
                usuarioService, "admin-matriz", SENHA, Role.ADMIN, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService, "operador-matriz", SENHA, Role.OPERADOR, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService, "gerente-matriz", SENHA, Role.GERENTE, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService, "encarregado-matriz", SENHA, Role.ENCARREGADO, true
        );

        adminToken = token("admin-matriz");
        operadorToken = token("operador-matriz");
        gerenteToken = token("gerente-matriz");
        encarregadoToken = token("encarregado-matriz");
    }

    @Test
    void gerenteAdministraEncarregadosEContratosMasNaoOperaEstoque()
            throws Exception {
        mockMvc.perform(get("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Encarregado do gerente",
                                "cpf", TestUsuarioFactory.proximoCpf(),
                                "celular", "11988887777",
                                "username", "encarregado-do-gerente",
                                "password", SENHA
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        long contratoId = criarContrato(
                gerenteToken,
                "Contrato do gerente",
                "Criado pelo gerente"
        );

        mockMvc.perform(get("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contratoPayload(
                                "Contrato editado pelo gerente",
                                "Descrição editada",
                                true
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome")
                        .value("Contrato editado pelo gerente"));

        mockMvc.perform(patch("/contratos/{id}/desativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch("/contratos/{id}/ativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        assertGetForbidden(gerenteToken, "/materiais");
        assertGetForbidden(gerenteToken, "/movimentacoes");
        assertGetForbidden(gerenteToken, "/notas-fiscais");

        mockMvc.perform(delete("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        assertFalse(contratoRepository.existsById(contratoId));
    }

    @Test
    void operadorConsultaContratosMasNaoPodeGerenciar() throws Exception {
        long contratoId = criarContrato(
                adminToken,
                "Contrato para leitura",
                "Disponível para o operador"
        );

        mockMvc.perform(get("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contratoPayload(
                                "Contrato negado",
                                "Operador não pode criar",
                                true
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contratoPayload(
                                "Edição negada",
                                "Operador não pode editar",
                                true
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/contratos/{id}/desativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/contratos/{id}/ativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void encarregadoNaoAcessaListagensGerais() throws Exception {
        assertGetForbidden(encarregadoToken, "/usuarios/encarregados");
        assertGetForbidden(encarregadoToken, "/contratos");
        assertGetForbidden(encarregadoToken, "/materiais");
        assertGetForbidden(encarregadoToken, "/movimentacoes");
        assertGetForbidden(encarregadoToken, "/notas-fiscais");
    }

    @Test
    void adminMantemAcessoAdministrativoEOperacional() throws Exception {
        for (String endpoint : new String[]{
                "/usuarios",
                "/usuarios/encarregados",
                "/materiais",
                "/contratos",
                "/movimentacoes",
                "/notas-fiscais"
        }) {
            mockMvc.perform(get(endpoint)
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isOk());
        }

        long contratoId = criarContrato(
                adminToken,
                "Contrato do administrador",
                "Gerenciamento administrativo preservado"
        );

        mockMvc.perform(put("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contratoPayload(
                                "Contrato administrativo editado",
                                "Edição administrativa preservada",
                                true
                        )))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/contratos/{id}/desativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/contratos/{id}/ativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
    }

    private long criarContrato(
            String token,
            String nome,
            String descricao
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contratoPayload(nome, descricao, true)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asLong();
    }

    private void assertGetForbidden(String token, String endpoint)
            throws Exception {
        mockMvc.perform(get(endpoint)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    private String contratoPayload(
            String nome,
            String descricao,
            boolean ativo
    ) throws Exception {
        return json(Map.of(
                "nome", nome,
                "descricao", descricao,
                "ativo", ativo
        ));
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", SENHA
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
