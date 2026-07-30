package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {

    private static final String SENHA_ADMIN = "senhaAdmin123";
    private static final String SENHA_OPERADOR = "senhaOperador123";
    private static final String SENHA_CONSULTA = "senhaConsulta123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

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
        funcionarioRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.criarUsuario("admin", SENHA_ADMIN, Role.ADMIN, true);
        usuarioService.criarUsuario("operador", SENHA_OPERADOR, Role.OPERADOR, true);
        usuarioService.criarUsuario("consulta", SENHA_CONSULTA, Role.CONSULTA, true);

        adminToken = token("admin", SENHA_ADMIN);
        operadorToken = token("operador", SENHA_OPERADOR);
        consultaToken = token("consulta", SENHA_CONSULTA);
    }

    @Test
    void materialNovoComecaComEstoqueZero() throws Exception {
        MvcResult result = mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
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

    @Test
    void cadastrarMaterialComNomeDuplicadoRetornaErro() throws Exception {
        criarMaterial("Capacete", 0);

        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
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
    void postMovimentacoesComEntradaERejeitado() throws Exception {
        mockMvc.perform(post("/movimentacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "funcionarioId", 1,
                                "contratoId", 1,
                                "materialId", 1,
                                "quantidade", 1,
                                "tipo", "ENTRADA"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void entradaAumentaEstoqueECriaMovimentacao() throws Exception {
        Material material = criarMaterial("Capacete", 0);

        mockMvc.perform(post("/movimentacoes/entrada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialId", material.getId(),
                                "quantidade", 7
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"));

        assertEquals(
                7,
                materialRepository.findById(material.getId()).orElseThrow()
                        .getQuantidadeEstoque()
        );
        assertEquals(1, movimentacaoRepository.findAll().size());
    }

    @Test
    void retiradaValidaReduzEstoque() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                operadorToken,
                contexto,
                TipoMovimentacao.RETIRADA,
                4
        ).andExpect(status().isCreated());

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
        ).andExpect(status().isCreated());

        assertEquals(
                8,
                materialRepository.findById(contexto.material().getId())
                        .orElseThrow()
                        .getQuantidadeEstoque()
        );
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

    @Test
    void quantidadeZeroERejeitada() throws Exception {
        mockMvc.perform(post("/movimentacoes/entrada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialId", 1,
                                "quantidade", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quantidadeNegativaERejeitada() throws Exception {
        mockMvc.perform(post("/movimentacoes/entrada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialId", 1,
                                "quantidade", -1
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quantidadeDezMilEUmERejeitada() throws Exception {
        mockMvc.perform(post("/movimentacoes/entrada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialId", 1,
                                "quantidade", 10001
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quantidadeDezMilEAceita() throws Exception {
        Material material = criarMaterial("Capacete", 0);

        mockMvc.perform(post("/movimentacoes/entrada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialId", material.getId(),
                                "quantidade", 10000
                        ))))
                .andExpect(status().isCreated());

        assertEquals(
                10000,
                materialRepository.findById(material.getId()).orElseThrow()
                        .getQuantidadeEstoque()
        );
    }

    @Test
    void cpfInvalidoERejeitado() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123",
                                "cargo", "Pedreiro"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cpfFormatadoESalvoSomenteComNumeros() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123.456.789-09",
                                "cargo", "Pedreiro"
                        ))))
                .andExpect(status().isCreated());

        Funcionario funcionario = funcionarioRepository
                .findByCpfNormalizado("12345678909")
                .orElseThrow();

        assertEquals("12345678909", funcionario.getCpf());
    }

    @Test
    void cpfsIguaisComFormatosDiferentesSaoDuplicados() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123.456.789-09",
                                "cargo", "Pedreiro"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Maria Silva",
                                "cpf", "12345678909",
                                "cargo", "Engenheira"
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
    void consultaNaoPodeRegistrarMovimentacao() throws Exception {
        ContextoMovimentacao contexto = criarContextoMovimentacao(10);

        registrarMovimentacao(
                consultaToken,
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
    void operadorNaoPodeCadastrarFuncionarioContratoOuMaterial() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "João Silva",
                                "cpf", "123.456.789-09",
                                "cargo", "Pedreiro"
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

        mockMvc.perform(post("/materiais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Capacete",
                                "descricao", "Capacete para uso em obra"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPossuiAcessoAdministrativo() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "novoOperador",
                                "password", "senhaForte123",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("novoOperador"))
                .andExpect(jsonPath("$.role").value("OPERADOR"));
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
        usuarioService.criarUsuario(
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
                criarFuncionario("João Silva", "12345678909"),
                criarContrato("Contrato A"),
                criarMaterial("Capacete", estoque)
        );
    }

    private Material criarMaterial(String nome, int estoque) {
        return materialRepository.save(
                new Material(nome, "Descrição do material", estoque)
        );
    }

    private Funcionario criarFuncionario(String nome, String cpf) {
        return funcionarioRepository.save(
                new Funcionario(nome, cpf, "Pedreiro")
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
        return mockMvc.perform(post("/movimentacoes")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "funcionarioId", contexto.funcionario().getId(),
                        "contratoId", contexto.contrato().getId(),
                        "materialId", contexto.material().getId(),
                        "quantidade", quantidade,
                        "tipo", tipo.name()
                ))));
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
            Funcionario funcionario,
            Contrato contrato,
            Material material
    ) {
    }
}
