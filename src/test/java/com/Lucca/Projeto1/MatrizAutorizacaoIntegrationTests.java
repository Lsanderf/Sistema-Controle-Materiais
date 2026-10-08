package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
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

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private ComprovanteMovimentacaoRepository comprovanteRepository;

    @Autowired
    private EvidenciaMovimentacaoRepository evidenciaRepository;

    private String adminToken;
    private String operadorToken;
    private String gerenteToken;
    private String encarregadoToken;

    @BeforeEach
    void prepararUsuarios() throws Exception {
        evidenciaRepository.deleteAll();
        comprovanteRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
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
    void gerenteConsultaContratosMasNaoAcessaMateriaisNemOperaEstoque()
            throws Exception {
        mockMvc.perform(get("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
        assertGetForbidden(gerenteToken, "/movimentacoes");
    }

    @Test
    void gerenteAdministraEncarregadosEGerenciaContratosSemGerenciarOutrosCadastros()
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("encarregado-do-gerente"));

        mockMvc.perform(get("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        long contratoId = criarContrato(
                adminToken,
                "Contrato administrativo",
                "Criado pelo administrador"
        );

        mockMvc.perform(get("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        long contratoCriadoPeloGerente = criarContrato(
                gerenteToken,
                "Contrato criado pelo gerente",
                "Criado para uma requisição"
        );

        mockMvc.perform(put("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contratoPayload(
                                "Contrato editado pelo gerente",
                                "Descrição editada",
                                true
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/contratos/{id}/desativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/contratos/{id}/ativar", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
        assertGetForbidden(gerenteToken, "/movimentacoes");
        assertGetForbidden(gerenteToken, "/notas-fiscais");

        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Material não autorizado",
                                "descricao", "O gerente não tem acesso a materiais"
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/contratos/{id}", contratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());

        assertTrue(contratoRepository.existsById(contratoId));
        assertTrue(contratoRepository.existsById(contratoCriadoPeloGerente));
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

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "OPERADOR"})
    void adminEOperadorPodemConsultarCriarEEditarMateriais(Role role) throws Exception {
        String authorization = bearer(role == Role.ADMIN ? adminToken : operadorToken);

        MvcResult result = mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete",
                                "descricao", "Capacete antigo"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeEstoque").value(0))
                .andReturn();
        long materialId = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asLong();

        mockMvc.perform(get("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(materialId));
        mockMvc.perform(get("/materiais/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Capacete"));
        mockMvc.perform(put("/materiais/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete de Segurança",
                                "descricao", "Modelo M"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Capacete de Segurança"))
                .andExpect(jsonPath("$.descricao").value("Modelo M"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(0));

        Material atualizado = materialRepository.findById(materialId).orElseThrow();
        assertEquals("Capacete de Segurança", atualizado.getNome());
        assertEquals("Modelo M", atualizado.getDescricao());
        assertEquals(0, atualizado.getQuantidadeEstoque());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"GERENTE", "ENCARREGADO"})
    void gerenteEEncarregadoNaoAcessamEndpointsDeMateriais(Role role) throws Exception {
        String authorization = bearer(role == Role.GERENTE ? gerenteToken : encarregadoToken);
        Material material = materialRepository.save(new Material("Capacete", "Capacete antigo", 15));
        String payload = json(Map.of(
                "nome", "Material não autorizado",
                "descricao", "Alteração não autorizada"
        ));

        mockMvc.perform(get("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/materiais/{id}", material.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/materiais/{id}", material.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        Material preservado = materialRepository.findById(material.getId()).orElseThrow();
        assertEquals("Capacete", preservado.getNome());
        assertEquals("Capacete antigo", preservado.getDescricao());
        assertEquals(15, preservado.getQuantidadeEstoque());
        assertEquals(1, materialRepository.count());
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
