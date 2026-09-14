package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.StatusNotaFiscal;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.Lucca.Projeto1.validation.ValidadorChaveAcessoNfe;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotaFiscalEntradaIntegrationTests {

    private static final String SENHA_ADMIN = "senhaAdmin123";
    private static final String SENHA_OPERADOR = "senhaOperador123";
    private static final String SENHA_CONSULTA = "senhaConsulta123";
    private static final String CNPJ_VALIDO = "11.222.333/0001-81";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private NotaFiscalEntradaRepository notaFiscalRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    private String adminToken;
    private String operadorToken;
    private String consultaToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        movimentacaoRepository.deleteAll();
        notaFiscalRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.criarUsuario(
                "admin",
                SENHA_ADMIN,
                Role.ADMIN,
                true
        );
        usuarioService.criarUsuario(
                "operador",
                SENHA_OPERADOR,
                Role.OPERADOR,
                true
        );
        usuarioService.criarUsuario(
                "consulta",
                SENHA_CONSULTA,
                Role.CONSULTA,
                true
        );

        adminToken = token("admin", SENHA_ADMIN);
        operadorToken = token("operador", SENHA_OPERADOR);
        consultaToken = token("consulta", SENHA_CONSULTA);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "OPERADOR"})
    void materialCriadoPelaApiSoRecebeEstoqueAoConfirmarNota(Role role) throws Exception {
        String authToken = role == Role.ADMIN ? adminToken : operadorToken;
        MvcResult cadastro = mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "CABO OPTICO DROP 1FO",
                                "descricao", "CABO OPTICO DROP 1FO"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeEstoque").value(0))
                .andReturn();
        Material material = materialRepository.findById(id(cadastro)).orElseThrow();
        assertEquals(0, estoque(material));
        assertEquals(0, movimentacaoRepository.count());
        assertEquals(0, notaFiscalRepository.count());

        Long notaId = id(criarNota(
                authToken,
                chave(80),
                List.of(item(material.getId(), 120, "2.50"))
        ));
        assertEquals(0, estoque(material));
        assertEquals(0, movimentacaoRepository.count());

        confirmar(notaId, authToken);
        assertEquals(120, estoque(material));
        assertEquals(1, movimentacaoRepository.count());
        assertEquals(TipoMovimentacao.ENTRADA,
                movimentacaoRepository.findAll().getFirst().getTipo());

        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(authToken)))
                .andExpect(status().isConflict());
        assertEquals(120, estoque(material));
        assertEquals(1, movimentacaoRepository.count());
    }

    @Test
    void criarNotaComDoisItensMantemEstoqueEMovimentacoesInalterados()
            throws Exception {
        Material luvas = criarMaterial("Luvas", 5);
        Material capacetes = criarMaterial("Capacetes", 2);

        MvcResult resultado = criarNota(
                operadorToken,
                chave(1),
                List.of(
                        item(luvas.getId(), 100, "8.50"),
                        item(capacetes.getId(), 20, "42.00")
                )
        );

        mockMvc.perform(get("/notas-fiscais/{id}", id(resultado))
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.cadastradaPorUsername").value("operador"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.valorTotal").value(1690.00))
                .andExpect(jsonPath("$.movimentacoes.length()").value(0));

        assertEquals(5, estoque(luvas));
        assertEquals(2, estoque(capacetes));
        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void confirmarNotaAtualizaEstoquesEGeraUmaEntradaPorItem()
            throws Exception {
        Material luvas = criarMaterial("Luvas", 5);
        Material capacetes = criarMaterial("Capacetes", 2);
        Long notaId = id(criarNota(
                operadorToken,
                chave(2),
                List.of(
                        item(luvas.getId(), 100, "8.50"),
                        item(capacetes.getId(), 20, "42.00")
                )
        ));

        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.dataEntrada").isNotEmpty())
                .andExpect(jsonPath("$.movimentacoes.length()").value(2))
                .andExpect(jsonPath("$.movimentacoes[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.movimentacoes[0].notaFiscalId").value(notaId))
                .andExpect(jsonPath("$.movimentacoes[0].usuarioUsername")
                        .value("operador"));

        assertEquals(105, estoque(luvas));
        assertEquals(22, estoque(capacetes));
        assertEquals(
                2,
                movimentacaoRepository.findByNotaFiscalIdOrderByIdAsc(notaId)
                        .size()
        );
        assertTrue(
                movimentacaoRepository.findByNotaFiscalIdOrderByIdAsc(notaId)
                        .stream()
                        .allMatch(movimentacao ->
                                movimentacao.getTipo() == TipoMovimentacao.ENTRADA
                                    && movimentacao.getNotaFiscal().getId()
                                        .equals(notaId)
                        )
        );

        mockMvc.perform(get("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(notaId))
                .andExpect(jsonPath("$[0].movimentacoes.length()").value(2));

        var nota = notaFiscalRepository.findById(notaId).orElseThrow();
        assertEquals(StatusNotaFiscal.CONFIRMADA, nota.getStatus());
        assertNotNull(nota.getDataEntrada());
    }

    @Test
    void confirmarNotaDuasVezesNaoDuplicaEstoqueNemMovimentacoes()
            throws Exception {
        Material material = criarMaterial("Furadeira", 0);
        Long notaId = id(criarNota(
                adminToken,
                chave(3),
                List.of(item(material.getId(), 3, "399.90"))
        ));

        confirmar(notaId, adminToken);

        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict());

        assertEquals(3, estoque(material));
        assertEquals(
                1,
                movimentacaoRepository.findByNotaFiscalIdOrderByIdAsc(notaId)
                        .size()
        );
    }

    @Test
    void chaveFormatadaENaoFormatadaSaoConsideradasDuplicadas()
            throws Exception {
        String chave = chave(4);
        criarNota(
                adminToken,
                formatarChave(chave),
                List.of()
        );

        mockMvc.perform(post("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(chave, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Já existe uma nota fiscal com essa chave de acesso"
                ));

        assertEquals(1, notaFiscalRepository.count());
        assertEquals(chave, notaFiscalRepository.findAll().getFirst()
                .getChaveAcesso());
    }

    @Test
    void backendRecusaChaveComDigitoVerificadorInvalido() throws Exception {
        String chaveValida = chave(18);
        int dvAlterado = (Character.digit(chaveValida.charAt(43), 10) + 1) % 10;
        String chaveInvalida = chaveValida.substring(0, 43) + dvAlterado;

        mockMvc.perform(post("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(chaveInvalida, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Chave de acesso da NF-e inválida. Verifique os números informados."
                ));

        assertEquals(0, notaFiscalRepository.count());
    }

    @Test
    void chaveComDvValidoContinuaNoFluxoDeCriacao() throws Exception {
        String chaveValida = "52060433009911002506550120000007800267301615";

        MvcResult resultado = criarNota(adminToken, chaveValida, List.of());

        assertEquals(
                chaveValida,
                jsonResponse(resultado).get("chaveAcesso").asText()
        );
        assertEquals(chaveValida, notaFiscalRepository.findAll()
                .getFirst().getChaveAcesso());
    }

    @Test
    void notaSemItensPodeSerRascunhoMasNaoPodeSerConfirmada()
            throws Exception {
        Long notaId = id(criarNota(adminToken, chave(5), List.of()));

        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict());

        assertEquals(
                StatusNotaFiscal.RASCUNHO,
                notaFiscalRepository.findById(notaId).orElseThrow().getStatus()
        );
        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void quantidadeInvalidaEValorNegativoSaoRejeitados()
            throws Exception {
        Material material = criarMaterial("Óculos", 0);

        for (int quantidade : List.of(0, -1)) {
            mockMvc.perform(post("/notas-fiscais")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(notaRequest(
                                    chave(10 + quantidade),
                                    List.of(item(
                                            material.getId(),
                                            quantidade,
                                            "1.00"
                                    ))
                            ))))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(
                                chave(12),
                                List.of(item(material.getId(), 1, "-0.01"))
                        ))))
                .andExpect(status().isBadRequest());

        assertEquals(0, notaFiscalRepository.count());
    }

    @Test
    void rascunhoPodeSerEditadoSemAlterarEstoque()
            throws Exception {
        Material luvas = criarMaterial("Luvas", 0);
        Material capacete = criarMaterial("Capacete", 7);
        Long notaId = id(criarNota(
                operadorToken,
                chave(13),
                List.of(item(luvas.getId(), 10, "5.00"))
        ));

        mockMvc.perform(put("/notas-fiscais/{id}", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(
                                chave(13),
                                List.of(item(capacete.getId(), 2, "40.00"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].materialId")
                        .value(capacete.getId()))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2));

        assertEquals(0, estoque(luvas));
        assertEquals(7, estoque(capacete));
        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void notaConfirmadaNaoPodeSerEditada() throws Exception {
        Material material = criarMaterial("Botina", 0);
        Long notaId = id(criarNota(
                adminToken,
                chave(14),
                List.of(item(material.getId(), 2, "80.00"))
        ));
        confirmar(notaId, adminToken);

        mockMvc.perform(put("/notas-fiscais/{id}", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(
                                chave(14),
                                List.of(item(material.getId(), 9, "1.00"))
                        ))))
                .andExpect(status().isConflict());

        assertEquals(2, estoque(material));
        assertEquals(
                1,
                movimentacaoRepository.findByNotaFiscalIdOrderByIdAsc(notaId)
                        .size()
        );
    }

    @Test
    void falhaNoSegundoItemFazRollbackCompletoDaConfirmacao()
            throws Exception {
        Material primeiro = criarMaterial("Primeiro material", 10);
        Material comEstoqueNoLimite = criarMaterial(
                "Material no limite",
                Integer.MAX_VALUE
        );
        Long notaId = id(criarNota(
                operadorToken,
                chave(15),
                List.of(
                        item(primeiro.getId(), 1, "1.00"),
                        item(comEstoqueNoLimite.getId(), 1, "1.00")
                )
        ));

        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isConflict());

        assertEquals(10, estoque(primeiro));
        assertEquals(Integer.MAX_VALUE, estoque(comEstoqueNoLimite));
        assertTrue(movimentacaoRepository.findAll().isEmpty());

        var nota = notaFiscalRepository.findById(notaId).orElseThrow();
        assertEquals(StatusNotaFiscal.RASCUNHO, nota.getStatus());
        assertNull(nota.getDataEntrada());
    }

    @Test
    void consultaPodeVisualizarMasNaoPodeCriarEditarOuConfirmar()
            throws Exception {
        Material material = criarMaterial("Máscara", 0);
        Long notaId = id(criarNota(
                operadorToken,
                chave(16),
                List.of(item(material.getId(), 1, "2.00"))
        ));

        mockMvc.perform(get("/notas-fiscais/{id}", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(chave(17), List.of()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/notas-fiscais/{id}", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(
                                chave(16),
                                List.of(item(material.getId(), 2, "2.00"))
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isForbidden());
    }

    private MvcResult criarNota(
            String token,
            String chave,
            List<Map<String, Object>> itens
    ) throws Exception {
        return mockMvc.perform(post("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(notaRequest(chave, itens))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn();
    }

    private void confirmar(Long notaId, String token) throws Exception {
        mockMvc.perform(post("/notas-fiscais/{id}/confirmar", notaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private Map<String, Object> notaRequest(
            String chave,
            List<Map<String, Object>> itens
    ) {
        return Map.of(
                "numero", "12345",
                "serie", "1",
                "chaveAcesso", chave,
                "fornecedor", "ABC Materiais Ltda",
                "cnpjFornecedor", CNPJ_VALIDO,
                "dataEmissao", LocalDate.now().toString(),
                "itens", itens
        );
    }

    private Map<String, Object> item(
            Long materialId,
            int quantidade,
            String valorUnitario
    ) {
        return Map.of(
                "materialId", materialId,
                "quantidade", quantidade,
                "valorUnitario", new BigDecimal(valorUnitario)
        );
    }

    private Material criarMaterial(String nome, int estoque) {
        return materialRepository.save(
                new Material(nome, "Descrição do material", estoque)
        );
    }

    private int estoque(Material material) {
        return materialRepository.findById(material.getId())
                .orElseThrow()
                .getQuantidadeEstoque();
    }

    private String chave(int valor) {
        String chaveSemDv = String.format("%043d", valor);
        return chaveSemDv
                + ValidadorChaveAcessoNfe.calcularDigitoVerificador(chaveSemDv);
    }

    private String formatarChave(String chave) {
        return chave.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private Long id(MvcResult result) throws Exception {
        return jsonResponse(result).get("id").asLong();
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return jsonResponse(result).get("token").asText();
    }

    private JsonNode jsonResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
