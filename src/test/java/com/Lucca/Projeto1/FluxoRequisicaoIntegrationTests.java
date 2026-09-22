package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.StatusRequisicao;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.imagem;
import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.assinatura;
import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.dados;
import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.evidencias.diretorio=target/test-evidencias-requisicoes")
@AutoConfigureMockMvc
class FluxoRequisicaoIntegrationTests {
    private static final String SENHA = "senhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsuarioService usuarios;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RequisicaoRepository requisicoes;
    @Autowired private MovimentacaoRepository movimentacoes;
    @Autowired private EvidenciaMovimentacaoRepository evidencias;
    @Autowired private ComprovanteMovimentacaoRepository comprovantes;
    @Autowired private NotaFiscalEntradaRepository notas;
    @Autowired private ContratoRepository contratos;
    @Autowired private MaterialRepository materiais;

    private String admin;
    private String operador;
    private String gerente;
    private String encarregado;
    private String outroEncarregado;
    private Long encarregadoId;
    private Contrato contrato;
    private Material material;

    @BeforeEach
    void preparar() throws Exception {
        evidencias.deleteAllInBatch();
        comprovantes.deleteAllInBatch();
        movimentacoes.deleteAllInBatch();
        requisicoes.deleteAllInBatch();
        notas.deleteAllInBatch();
        contratos.deleteAllInBatch();
        materiais.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();

        criarUsuario("Administrador", "11144477735", "admin", Role.ADMIN);
        criarUsuario("Operador", "52998224725", "operador", Role.OPERADOR);
        criarUsuario("Gerente", "12345678909", "gerente", Role.GERENTE);
        criarUsuario("Encarregado", "93541134780", "encarregado", Role.ENCARREGADO);
        criarUsuario("Outro encarregado", "39053344705", "outro", Role.ENCARREGADO);
        admin = token("admin");
        operador = token("operador");
        gerente = token("gerente");
        encarregado = token("encarregado");
        outroEncarregado = token("outro");
        encarregadoId = usuarioRepository.findByUsernameIgnoreCase("encarregado").orElseThrow().getId();
        contrato = contratos.save(new Contrato("Contrato A", "Obra A", true));
        material = materiais.save(new Material("Cabo", "Cabo 2,5 mm", 20));
    }

    @Test
    void multiplasRetiradasExigemFinalizacaoExplicitaEConfirmacaoDoProprietario() throws Exception {
        long requisicaoId = criarRequisicao();
        registrarRetirada(requisicaoId, 3, "retirada-1").andExpect(status().isCreated());
        statusRequisicao(requisicaoId, gerente, "PENDENTE");
        mvc.perform(post("/requisicoes/{id}/confirmar", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(encarregado)))
                .andExpect(status().isConflict());

        registrarRetirada(requisicaoId, 2, "retirada-2").andExpect(status().isCreated());
        statusRequisicao(requisicaoId, gerente, "PENDENTE");
        mvc.perform(post("/requisicoes/{id}/finalizar-atendimento", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));

        mvc.perform(post("/requisicoes/{id}/confirmar", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(outroEncarregado)))
                .andExpect(status().isConflict());
        mvc.perform(post("/requisicoes/{id}/confirmar", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(encarregado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.confirmadoPor.username").value("encarregado"))
                .andExpect(jsonPath("$.confirmadoEm").isNotEmpty());
        assertEquals(15, materiais.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
        registrarRetirada(requisicaoId, 1, "apos-finalizacao").andExpect(status().isConflict());
    }

    @Test
    void finalizarSemRetiradaFalhaEEstornoDaUltimaRetiradaReabreAtendimento() throws Exception {
        long requisicaoId = criarRequisicao();
        mvc.perform(post("/requisicoes/{id}/finalizar-atendimento", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isConflict());
        long primeira = id(registrarRetirada(requisicaoId, 2, "r-a").andReturn());
        long segunda = id(registrarRetirada(requisicaoId, 2, "r-b").andReturn());
        mvc.perform(post("/requisicoes/{id}/finalizar-atendimento", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isOk());
        estornar(primeira).andExpect(status().isCreated());
        statusRequisicao(requisicaoId, admin, "AGUARDANDO_CONFIRMACAO");
        estornar(segunda).andExpect(status().isCreated());
        statusRequisicao(requisicaoId, admin, "PENDENTE");
        assertEquals(20, materiais.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void segurancaPerfisCadastroDelegadoEIdorSaoAplicadosNoBackend() throws Exception {
        mvc.perform(post("/usuarios/encarregados").header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Novo", "cpf", "86288366757", "celular", "31999990009", "username", "novo", "password", SENHA, "role", "ADMIN"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/contratos").header(HttpHeaders.AUTHORIZATION, bearer(operador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Negado", "descricao", "Negado", "ativo", true))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/materiais").header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isForbidden());
        long requisicaoId = criarRequisicao();
        mvc.perform(get("/requisicoes/{id}", requisicaoId).header(HttpHeaders.AUTHORIZATION, bearer(outroEncarregado)))
                .andExpect(status().isConflict());
    }

    @Test
    void requisicaoCriadaPeloGerenteEListadaSomenteParaOEncarregadoResponsavel() throws Exception {
        long requisicaoId = criarRequisicao();

        var persistida = requisicoes.findById(requisicaoId).orElseThrow();
        assertEquals(usuarioRepository.findByUsernameIgnoreCase("gerente").orElseThrow().getId(),
                persistida.getGerente().getId());
        assertEquals(encarregadoId, persistida.getEncarregado().getId());
        assertEquals(contrato.getId(), persistida.getContrato().getId());
        assertEquals(StatusRequisicao.PENDENTE, persistida.getStatus());

        MvcResult minhas = mvc.perform(get("/requisicoes/minhas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(encarregado)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(requisicaoId, json(minhas).get(0).get("id").asLong());

        MvcResult deOutroEncarregado = mvc.perform(get("/requisicoes/minhas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(outroEncarregado)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(0, json(deOutroEncarregado).size());

        mvc.perform(get("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requisicaoId));
        mvc.perform(get("/requisicoes/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requisicaoId));
    }

    @Test
    void contratosOperacionaisNaoExibemCpfENormalizamCelular() throws Exception {
        mvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpf").isNotEmpty());

        mvc.perform(get("/usuarios/encarregados").header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpf").doesNotExist());
        mvc.perform(get("/usuarios/encarregados").header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpf").doesNotExist());
        mvc.perform(get("/usuarios/encarregados").header(HttpHeaders.AUTHORIZATION, bearer(encarregado)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/usuarios/encarregados").header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Novo resumo", "cpf", "86288366757",
                                "celular", "(31) 99999-9999", "username", "novo-resumo",
                                "password", SENHA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.celular").value("31999999999"));
        assertEquals("31999999999", usuarioRepository.findByUsernameIgnoreCase("novo-resumo")
                .orElseThrow().getCelular());
    }

    @Test
    void cadastroDeUsuarioRejeitaUsernameCpfDuplicadosEPapelLegado() throws Exception {
        String base = json(Map.of(
                "nome", "Usuário novo",
                "cpf", "86288366757",
                "celular", "31999990009",
                "username", "usuario-novo",
                "password", SENHA,
                "role", "OPERADOR"
        ));
        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(base))
                .andExpect(status().isCreated());

        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Outro", "cpf", "16899535009", "celular", "31999990008",
                                "username", "USUARIO-NOVO", "password", SENHA, "role", "OPERADOR"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Outro", "cpf", "862.883.667-57", "celular", "31999990008",
                                "username", "outro-nome", "password", SENHA, "role", "OPERADOR"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Legado", "cpf", "11144477735", "celular", "31999990008",
                                "username", "legado", "password", SENHA, "role", "CONSULTA"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devolucaoExigeFotoAssinaturaEUsaSaldoDoEncarregado() throws Exception {
        registrarRetirada(null, 5, "retirada-devolucao").andExpect(status().isCreated());
        String payload = json(Map.of("encarregadoId", encarregadoId, "contratoId", contrato.getId(),
                "materialId", material.getId(), "quantidade", 2, "tipo", "DEVOLUCAO"));
        mvc.perform(multipart("/movimentacoes").file(dados(payload)).file(assinatura())
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isConflict());
        MockMultipartFile foto = new MockMultipartFile("foto", "retorno.png", "image/png", imagem("png", false));
        mvc.perform(multipart("/movimentacoes").file(dados(payload)).file(assinatura()).file(foto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isCreated());
        assertEquals(17, materiais.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
    }

    private void criarUsuario(String nome, String cpf, String username, Role role) {
        TestUsuarioFactory.criarUsuario(usuarios, nome, cpf, "31999999999", username, SENHA, role, true);
    }

    private long criarRequisicao() throws Exception {
        MvcResult result = mvc.perform(post("/requisicoes").header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("descricao", "10 cabos e ferramentas", "encarregadoId", encarregadoId, "contratoId", contrato.getId()))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE")).andReturn();
        return id(result);
    }

    private org.springframework.test.web.servlet.ResultActions registrarRetirada(Long requisicaoId, int quantidade, String key) throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("encarregadoId", encarregadoId); payload.put("contratoId", contrato.getId());
        payload.put("materialId", material.getId()); payload.put("quantidade", quantidade); payload.put("tipo", "RETIRADA");
        if (requisicaoId != null) payload.put("requisicaoId", requisicaoId);
        return mvc.perform(movimentacaoAssinada(json(payload))
                .header(HttpHeaders.AUTHORIZATION, bearer(operador)).header("Idempotency-Key", key));
    }

    private org.springframework.test.web.servlet.ResultActions estornar(long id) throws Exception {
        return mvc.perform(post("/movimentacoes/{id}/estorno", id).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("justificativa", "Correção operacional"))));
    }

    private void statusRequisicao(long id, String token, String statusEsperado) throws Exception {
        mvc.perform(get("/requisicoes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(statusEsperado));
    }

    private String token(String username) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", SENHA))))
                .andExpect(status().isOk()).andReturn();
        return json(result).get("token").asText();
    }

    private long id(MvcResult result) throws Exception { return json(result).get("id").asLong(); }
    private JsonNode json(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private String bearer(String token) { return "Bearer " + token; }
}
