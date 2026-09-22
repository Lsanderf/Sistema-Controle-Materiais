package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.service.MovimentacaoService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {

    private static final String SENHA_ADMIN = "senhaAdmin123";
    private static final String SENHA_OPERADOR = "senhaOperador123";
    private static final String SENHA_GERENTE = "senhaConsulta123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private UsuarioRepository encarregadoRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private EvidenciaMovimentacaoRepository evidenciaRepository;

    @Autowired
    private ComprovanteMovimentacaoRepository comprovanteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    private String adminToken;
    private String operadorToken;
    private String gerenteToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        evidenciaRepository.deleteAll();
        comprovanteRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        encarregadoRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(usuarioService, "admin", SENHA_ADMIN, Role.ADMIN, true);
        TestUsuarioFactory.criarUsuario(usuarioService, "operador", SENHA_OPERADOR, Role.OPERADOR, true);
        TestUsuarioFactory.criarUsuario(usuarioService, "gerente", SENHA_GERENTE, Role.GERENTE, true);

        adminToken = token("admin", SENHA_ADMIN);
        operadorToken = token("operador", SENHA_OPERADOR);
        gerenteToken = token("gerente", SENHA_GERENTE);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "OPERADOR"})
    void materialNovoComecaComEstoqueZero(Role role) throws Exception {
        MvcResult result = mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(role == Role.ADMIN ? adminToken : operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete",
                                "descricao", "Capacete para uso em obra"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeEstoque").value(0))
                .andReturn();

        Long id = jsonResponse(result).get("id").asLong();

        assertEquals(
                0,
                materialRepository.findById(id).orElseThrow()
                        .getQuantidadeEstoque()
        );
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "OPERADOR"})
    void cadastrarMaterialComNomeDuplicadoRetornaErro(Role role) throws Exception {
        criarMaterial("Capacete", 0);

        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(role == Role.ADMIN ? adminToken : operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "capacete",
                                "descricao", "Outro capacete"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void atualizarMaterialParaNomeDeOutroMaterialRetornaErro() throws Exception {
        Material capacete = criarMaterial("Capacete", 0);
        criarMaterial("Luvas", 0);

        mockMvc.perform(put("/materiais/{id}", capacete.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "luvas",
                                "descricao", "Descrição atualizada"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void atualizarContratoParaNomeDeOutroContratoRetornaErro() throws Exception {
        Contrato contrato = criarContrato("Contrato A");
        criarContrato("Contrato B");

        mockMvc.perform(put("/contratos/{id}", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "contrato b",
                                "descricao", "Descrição atualizada",
                                "ativo", true
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void postMovimentacoesComEntradaERejeitadoENaoAlteraEstoque()
            throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        mockMvc.perform(post("/movimentacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoId", contexto.encarregado().getId(),
                                "contratoId", contexto.contrato().getId(),
                                "materialId", contexto.material().getId(),
                                "quantidade", 7,
                                "tipo", "ENTRADA"
                        ))))
                .andExpect(status().isConflict());

        assertEquals(10, estoque(contexto.material()));
        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void endpointPublicoDeEntradaManualNaoExisteENaoAlteraEstoque()
            throws Exception {
        Material material = criarMaterial("Capacete", 0);

        mockMvc.perform(post("/movimentacoes/entrada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialId", material.getId(),
                                "quantidade", 7
                        ))))
                .andExpect(status().isMethodNotAllowed());

        assertEquals(
                0,
                materialRepository.findById(material.getId()).orElseThrow()
                        .getQuantidadeEstoque()
        );
        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void movimentacaoServiceNaoExpoeMetodoPublicoDeEntradaManual() {
        assertTrue(
                Arrays.stream(MovimentacaoService.class.getMethods())
                        .noneMatch(method ->
                                method.getName().equals("registrarEntrada")
                        )
        );
    }

    @Test
    void usuarioResponsavelNaoPodeSerEscolhidoPeloRequest() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);
        Usuario admin = usuarioRepository
                .findByUsernameIgnoreCase("admin")
                .orElseThrow();

        mockMvc.perform(post("/movimentacoes")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoId", contexto.encarregado().getId(),
                                "contratoId", contexto.contrato().getId(),
                                "materialId", contexto.material().getId(),
                                "quantidade", 1,
                                "tipo", "RETIRADA",
                                "usuarioId", admin.getId()
                        ))))
                .andExpect(status().isBadRequest());

        assertTrue(movimentacaoRepository.findAll().isEmpty());
        assertEquals(10, estoque(contexto.material()));
    }

    @Test
    void movimentacaoAntigaSemUsuarioContinuaSendoRetornada() throws Exception {
        Material material = criarMaterial("Capacete", 10);
        Movimentacao antiga = new Movimentacao();
        antiga.setMaterial(material);
        antiga.setQuantidade(2);
        antiga.setTipo(TipoMovimentacao.ENTRADA);
        antiga.setDataMovimentacao(LocalDateTime.now());
        antiga.setRegistradoPor(null);
        antiga = movimentacaoRepository.save(antiga);

        MvcResult result = mockMvc.perform(
                        get("/movimentacoes/{id}", antiga.getId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = jsonResponse(result);
        assertTrue(response.get("usuarioId").isNull());
        assertTrue(response.get("usuarioUsername").isNull());
        assertNull(
                movimentacaoRepository.findById(antiga.getId())
                        .orElseThrow()
                        .getRegistradoPor()
        );
    }

    @Test
    void retiradaValidaReduzEstoque() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                4
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioUsername").value("operador"))
                .andExpect(jsonPath("$.usuarioId").isNumber());

        assertEquals(
                6,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );
    }

    @Test
    void retiradaMaiorQueEstoqueERejeitadaENaoAlteraBanco() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(5);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                6
        ).andExpect(status().isConflict());

        assertEquals(
                5,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );
        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void retiradaComQuantidadeZeroERejeitadaENaoAlteraBanco() throws Exception {
        // ARRANGE
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        // ACT
        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                0
        ).andExpect(status().isBadRequest());

        // ASSERT
        assertEquals(
                10,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );

        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void retiradaComQuantidadeNegativaERejeitadaENaoAlteraBanco() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                -1
        ).andExpect(status().isBadRequest());

        assertEquals(
                10,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );

        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void devolucaoValidaAumentaEstoque() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);
        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                6
        ).andExpect(status().isCreated());

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.DEVOLUCAO,
                4
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioUsername").value("operador"))
                .andExpect(jsonPath("$.usuarioId").isNumber());

        assertEquals(
                8,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );
    }

    @Test
    void devolucaoSemRetiradaAnteriorERejeitadaENaoAlteraBanco() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.DEVOLUCAO,
                1
        ).andExpect(status().isConflict());

        assertEquals(
                10,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );

        assertTrue(movimentacaoRepository.findAll().isEmpty());
    }

    @Test
    void devolucaoMaiorQueSaldoRetiradoERejeitada() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);
        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                4
        ).andExpect(status().isCreated());

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.DEVOLUCAO,
                5
        ).andExpect(status().isConflict());

        assertEquals(
                6,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "OPERADOR"})
    void cadastroDeMaterialNaoPermiteDefinirEstoqueInicialPeloRequest(Role role)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(role == Role.ADMIN ? adminToken : operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete",
                                "descricao", "Capacete para uso em obra",
                                "quantidadeEstoque", 10000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeEstoque").value(0))
                .andReturn();

        assertEquals(
                0,
                materialRepository.findById(
                        jsonResponse(result).get("id").asLong()
                ).orElseThrow().getQuantidadeEstoque()
        );
    }

    @Test
    void atualizacaoDeMaterialNaoPermiteAlterarEstoquePeloRequest()
            throws Exception {
        Material material = criarMaterial("Capacete", 0);

        mockMvc.perform(put("/materiais/{id}", material.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete atualizado",
                                "descricao", "Descricao atualizada",
                                "quantidadeEstoque", 10000
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(0));

        assertEquals(
                0,
                materialRepository.findById(material.getId()).orElseThrow()
                        .getQuantidadeEstoque()
        );
    }

    @Test
    void cpfInvalidoERejeitado() throws Exception {
        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123",
                                "celular", "11999999999",
                                "username", "joao-invalido",
                                "password", "senhaForte123"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cpfFormatadoESalvoSomenteComNumeros() throws Exception {
        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123.456.789-09",
                                "celular", "11999999999",
                                "username", "joao-formatado",
                                "password", "senhaForte123"
                        ))))
                .andExpect(status().isCreated());

        Usuario encarregado = encarregadoRepository
                .findByCpf("12345678909")
                .orElseThrow();

        assertEquals("12345678909", encarregado.getCpf());
    }

    @Test
    void cpfsIguaisComFormatosDiferentesSaoDuplicados() throws Exception {
        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123.456.789-09",
                                "celular", "11999999999",
                                "username", "joao-duplicado",
                                "password", "senhaForte123"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Maria Silva",
                                "cpf", "12345678909",
                                "celular", "11988888888",
                                "username", "maria-duplicada",
                                "password", "senhaForte123"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void materialSemDescricaoRetornaBadRequest() throws Exception {
        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Capacete"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists());
    }

    @Test
    void contratoSemDescricaoRetornaBadRequest() throws Exception {
        mockMvc.perform(post("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Contrato A",
                                "ativo", true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists());
    }

    @Test
    void requisicaoSemTokenRecebeUnauthorized() throws Exception {
        mockMvc.perform(get("/materiais"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gerenteNaoPodeRegistrarMovimentacao() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                gerenteToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                1
        ).andExpect(status().isForbidden());
    }

    @Test
    void operadorPodeRegistrarMovimentacao() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                1
        ).andExpect(status().isCreated());
    }

    @Test
    void operadorNaoPodeCadastrarUsuarioOuContrato() throws Exception {
        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123.456.789-09",
                                "celular", "11999999999",
                                "username", "joao-operador",
                                "password", "senhaForte123"
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/contratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Contrato A",
                                "descricao", "Contrato de obra",
                                "ativo", true
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerenteNaoPodeCadastrarMaterial() throws Exception {
        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete",
                                "descricao", "Capacete para uso em obra"
                        ))))
                .andExpect(status().isForbidden());

        assertEquals(0, materialRepository.count());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OPERADOR", "GERENTE"})
    void perfisSemAcessoAdministrativoNaoPodemAlterarMateriais(Role role)
            throws Exception {
        Material material = criarMaterial("Capacete", 8);
        String authorization = bearer(role == Role.OPERADOR ? operadorToken : gerenteToken);

        for (var request : List.of(
                put("/materiais/{id}", material.getId()),
                patch("/materiais/{id}", material.getId()),
                delete("/materiais/{id}", material.getId()),
                post("/materiais/{id}", material.getId()),
                post("/materiais/{id}/inativar", material.getId()),
                patch("/materiais/{id}/inativar", material.getId())
        )) {
            mockMvc.perform(request
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nome", "Material alterado",
                                    "descricao", "Descrição alterada",
                                    "quantidadeEstoque", 100,
                                    "ativo", false
                            ))))
                    .andExpect(status().isForbidden());
        }

        Material preservado = materialRepository.findById(material.getId()).orElseThrow();
        assertEquals(material.getNome(), preservado.getNome());
        assertEquals(material.getDescricao(), preservado.getDescricao());
        assertEquals(8, preservado.getQuantidadeEstoque());
        assertEquals(1, materialRepository.count());
    }

    @Test
    void apenasAdminEOperadorPodemConsultarMateriais() throws Exception {
        Material material = criarMaterial("Capacete", 8);

        for (String authToken : List.of(adminToken, operadorToken)) {
            mockMvc.perform(get("/materiais")
                            .header(HttpHeaders.AUTHORIZATION, bearer(authToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(material.getId()));
            mockMvc.perform(get("/materiais/{id}", material.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(authToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Capacete"));
        }

        mockMvc.perform(get("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPossuiAcessoAdministrativo() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Novo Operador",
                                "cpf", "11144477735",
                                "celular", "11999999999",
                                "username", "novoOperador",
                                "password", "senhaForte123",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("novoOperador"))
                .andExpect(jsonPath("$.role").value("OPERADOR"));
    }

    @Test
    void adminConsegueListarUsuariosSemExporCredenciais() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].username").isString())
                .andExpect(jsonPath("$[0].role").isString())
                .andExpect(jsonPath("$[0].ativo").isBoolean())
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void operadorEConsultaNaoPodemListarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/usuarios")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(gerenteToken)
                        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsegueConsultarUsuarioPorId() throws Exception {
        Usuario operador = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();

        mockMvc.perform(get("/usuarios/{id}", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(operador.getId()))
                .andExpect(jsonPath("$.username").value("operador"))
                .andExpect(jsonPath("$.role").value("OPERADOR"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void usuarioPodeSerEditadoSemTrocarSenha() throws Exception {
        Usuario operador = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();
        String hashOriginal = operador.getSenha();

        mockMvc.perform(put("/usuarios/{id}", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", operador.getNome(),
                                "cpf", operador.getCpf(),
                                "celular", operador.getCelular(),
                                "username", "operador_estoque",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("operador_estoque"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario atualizado = usuarioRepository
                .findByUsernameIgnoreCase("operador_estoque")
                .orElseThrow();
        assertEquals(hashOriginal, atualizado.getSenha());
    }

    @Test
    void usuarioPodeSerEditadoComNovaSenha() throws Exception {
        Usuario operador = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();

        mockMvc.perform(put("/usuarios/{id}", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", operador.getNome(),
                                "cpf", operador.getCpf(),
                                "celular", operador.getCelular(),
                                "username", "operador",
                                "role", "OPERADOR",
                                "novaSenha", "senhaNova123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "operador",
                                "password", SENHA_OPERADOR
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "operador",
                                "password", "senhaNova123"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void usernameDuplicadoERejeitadoNaEdicao() throws Exception {
        Usuario operador = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();

        mockMvc.perform(put("/usuarios/{id}", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", operador.getNome(),
                                "cpf", operador.getCpf(),
                                "celular", operador.getCelular(),
                                "username", "gerente",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void usuarioPodeSerDesativadoEAtivado() throws Exception {
        Usuario operador = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();

        mockMvc.perform(patch("/usuarios/{id}/desativar", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch("/usuarios/{id}/ativar", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void administradorNaoPodeDesativarPropriaConta() throws Exception {
        Usuario admin = usuarioRepository
                .findByUsernameIgnoreCase("admin")
                .orElseThrow();

        mockMvc.perform(patch("/usuarios/{id}/desativar", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Você não pode desativar sua própria conta"
                ));
    }

    @Test
    void ultimoAdminAtivoNaoPodeSerDesativadoNemPerderRole() throws Exception {
        Usuario admin = usuarioRepository
                .findByUsernameIgnoreCase("admin")
                .orElseThrow();

        assertThrows(
                RegraNegocioException.class,
                () -> usuarioService.desativar(admin.getId(), "outro-admin")
        );

        mockMvc.perform(put("/usuarios/{id}", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", admin.getNome(),
                                "cpf", admin.getCpf(),
                                "celular", admin.getCelular(),
                                "username", "admin",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void tokenAntigoDeUsuarioDesativadoDeixaDeFuncionar() throws Exception {
        Usuario operador = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();

        mockMvc.perform(patch("/usuarios/{id}/desativar", operador.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/materiais")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(
                        "Usuário inativo ou não encontrado. Entre novamente"
                ));
    }

    @Test
    void loginComSenhaCorretaRetornaJwt() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", SENHA_ADMIN
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginComSenhaIncorretaRetornaUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", "senhaErrada123"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInativoNaoConsegueLogin() throws Exception {
        TestUsuarioFactory.criarUsuario(usuarioService,
                "inativo",
                "senhaInativa123",
                Role.OPERADOR,
                false
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "inativo",
                                "password", "senhaInativa123"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duasRetiradasConcorrentesNaoRetiramMaisQueEstoqueDisponivel()
            throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);

        Callable<Integer> retirada = () -> {
            inicio.await(5, TimeUnit.SECONDS);
            return registrarMovimentacaoStatus(
                    operadorToken,
                    contexto,
                    TipoMovimentacao.RETIRADA,
                    7
            );
        };

        try {
            Future<Integer> primeira = executor.submit(retirada);
            Future<Integer> segunda = executor.submit(retirada);
            inicio.countDown();

            List<Integer> statusCodes = List.of(
                    primeira.get(10, TimeUnit.SECONDS),
                    segunda.get(10, TimeUnit.SECONDS)
            );

            long sucessos = statusCodes.stream()
                    .filter(status -> status == 201)
                    .count();
            long conflitos = statusCodes.stream()
                    .filter(status -> status == 409)
                    .count();

            assertEquals(1, sucessos);
            assertEquals(1, conflitos);
            assertEquals(
                    3,
                    materialRepository.findById(contexto.material().getId())
                            .orElseThrow()
                            .getQuantidadeEstoque()
            );
            assertEquals(
                    1,
                    movimentacaoRepository.findAll().stream()
                            .filter(movimentacao ->
                                    movimentacao.getTipo()
                                            == TipoMovimentacao.RETIRADA
                            )
                            .count()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private ContextoMovimentacao criarContextoMovimentacao(int estoque) {
        return new ContextoMovimentacao(
                criarUsuario("João Silva", "12345678909"),
                criarContrato("Contrato A"),
                criarMaterial("Capacete", estoque)
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

    private Usuario criarUsuario(String nome, String cpf) {
        return encarregadoRepository.save(
                TestUsuarioFactory.encarregado(nome, cpf, "Pedreiro")
        );
    }

    private Contrato criarContrato(String nome) {
        return contratoRepository.save(
                new Contrato(nome, "Descrição do contrato", true)
        );
    }

    private org.springframework.test.web.servlet.ResultActions registrarMovimentacao(
            String token,
            ContextoMovimentacao contexto,
            TipoMovimentacao tipo,
            int quantidade
    ) throws Exception {
        return mockMvc.perform(movimentacaoAssinada(json(Map.of(
                        "encarregadoId", contexto.encarregado().getId(),
                        "contratoId", contexto.contrato().getId(),
                        "materialId", contexto.material().getId(),
                        "quantidade", quantidade,
                        "tipo", tipo.name()
                )))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private int registrarMovimentacaoStatus(
            String token,
            ContextoMovimentacao contexto,
            TipoMovimentacao tipo,
            int quantidade
    ) throws Exception {
        return registrarMovimentacao(token, contexto, tipo, quantidade)
                .andReturn()
                .getResponse()
                .getStatus();
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

    private record ContextoMovimentacao(
            Usuario encarregado,
            Contrato contrato,
            Material material
    ) {
    }
    @Test
    void retiradaValidaCriaExatamenteUmaMovimentacao() throws Exception {
        // ARRANGE
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        // ACT
        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                4
        ).andExpect(status().isCreated());

        // ASSERT
        assertEquals(6, estoque(contexto.material()));

        List<Movimentacao> movimentacoes = movimentacaoRepository.findAll();

        assertEquals(1, movimentacoes.size());

        Movimentacao movimentacao = movimentacoes.get(0);

        assertEquals(TipoMovimentacao.RETIRADA, movimentacao.getTipo());
        assertEquals(4, movimentacao.getQuantidade());
        assertEquals(
                contexto.material().getId(),
                movimentacao.getMaterial().getId()
        );
    }

    @Test
    void retiradaValidaCriaComprovanteEEvidencia() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                2
        ).andExpect(status().isCreated());

        assertEquals(8, estoque(contexto.material()));

        assertEquals(1, movimentacaoRepository.count());
        assertEquals(1, comprovanteRepository.count());
        assertEquals(1, evidenciaRepository.count());
    }

    @Test
    void retiradaInvalidaNaoCriaMovimentacaoComprovanteNemEvidencia()
            throws Exception {

        ContextoMovimentacao contexto = criarContextoMovimentacao(5);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                10
        ).andExpect(status().isConflict());

        assertEquals(5, estoque(contexto.material()));

        assertEquals(0, movimentacaoRepository.count());
        assertEquals(0, comprovanteRepository.count());
        assertEquals(0, evidenciaRepository.count());
    }

    @Test
    void tentativaDeMovimentacaoPorConsultaNaoAlteraNada() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                gerenteToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                3
        ).andExpect(status().isForbidden());

        assertEquals(10, estoque(contexto.material()));

        assertEquals(0, movimentacaoRepository.count());
        assertEquals(0, comprovanteRepository.count());
        assertEquals(0, evidenciaRepository.count());
    }
}
