package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.NotaFiscalEntrada;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.StatusNotaFiscal;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EstornoMovimentacaoIntegrationTests {

    private static final String SENHA = "senhaTeste123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private UsuarioRepository encarregadoRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;
    @Autowired private EvidenciaMovimentacaoRepository evidenciaRepository;
    @Autowired private ComprovanteMovimentacaoRepository comprovanteRepository;
    @Autowired private NotaFiscalEntradaRepository notaFiscalRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioService usuarioService;

    private String adminToken;
    private String operadorToken;
    private String gerenteToken;
    private Usuario admin;

    @BeforeEach
    void prepararBanco() throws Exception {
        limparBanco();

        TestUsuarioFactory.criarUsuario(usuarioService, "admin", SENHA, Role.ADMIN, true);
        TestUsuarioFactory.criarUsuario(usuarioService, "operador", SENHA, Role.OPERADOR, true);
        TestUsuarioFactory.criarUsuario(usuarioService, "gerente", SENHA, Role.GERENTE, true);
        admin = usuarioRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        adminToken = token("admin");
        operadorToken = token("operador");
        gerenteToken = token("gerente");
    }

    @AfterEach
    void limparBanco() {
        evidenciaRepository.deleteAllInBatch();
        comprovanteRepository.deleteAllInBatch();
        movimentacaoRepository.deleteAllInBatch();
        notaFiscalRepository.deleteAllInBatch();
        encarregadoRepository.deleteAllInBatch();
        contratoRepository.deleteAllInBatch();
        materialRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void estornoTotalDeRetiradaRestauraEstoquePreservaOriginalEGeraComprovante()
            throws Exception {
        Contexto contexto = criarContexto("Retirada", "11144477735", 10);
        long origemId = registrar(contexto, TipoMovimentacao.RETIRADA, 4);
        Movimentacao originalAntes = movimentacaoRepository.findById(origemId).orElseThrow();
        LocalDateTime dataOriginal = originalAntes.getDataMovimentacao();

        MvcResult result = estornar(adminToken, origemId, "  Quantidade registrada incorretamente  ", "estorno-r-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ESTORNO_RETIRADA"))
                .andExpect(jsonPath("$.movimentacaoOrigemId").value(origemId))
                .andReturn();

        long estornoId = jsonResponse(result).get("id").asLong();
        assertEquals(10, estoque(contexto.material()));
        assertEquals(2, movimentacaoRepository.count());
        assertEquals(2, comprovanteRepository.count());
        assertEquals(1, evidenciaRepository.count(), "o estorno administrativo não exige assinatura");

        Movimentacao originalDepois = movimentacaoRepository.findById(origemId).orElseThrow();
        assertEquals(TipoMovimentacao.RETIRADA, originalDepois.getTipo());
        assertEquals(4, originalDepois.getQuantidade());
        assertEquals(dataOriginal, originalDepois.getDataMovimentacao());

        Movimentacao estorno = movimentacaoRepository.findById(estornoId).orElseThrow();
        assertEquals(origemId, estorno.getMovimentacaoOrigem().getId());
        assertEquals("Quantidade registrada incorretamente", estorno.getObservacao());
        assertEquals(admin.getId(), estorno.getRegistradoPor().getId());
        assertNotNull(estorno.getDataFinalizacao());

        mockMvc.perform(get("/movimentacoes/{id}", origemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estornada").value(true))
                .andExpect(jsonPath("$.estornoId").value(estornoId));

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", estornoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movimentacaoOrigemId").value(origemId))
                .andExpect(jsonPath("$.observacao").value("Quantidade registrada incorretamente"));
    }

    @Test
    void estornoDeDevolucaoRetiraDoEstoqueAQuantidadeOriginal() throws Exception {
        Contexto contexto = criarContexto("Devolucao", "52998224725", 10);
        registrar(contexto, TipoMovimentacao.RETIRADA, 6);
        long devolucaoId = registrar(contexto, TipoMovimentacao.DEVOLUCAO, 4);
        assertEquals(8, estoque(contexto.material()));

        estornar(adminToken, devolucaoId, "Devolução lançada por engano", "estorno-d-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ESTORNO_DEVOLUCAO"));

        assertEquals(4, estoque(contexto.material()));
        assertTrue(movimentacaoRepository.findByMovimentacaoOrigemId(devolucaoId).isPresent());
    }

    @Test
    void justificativaVaziaNaoPersisteNemAlteraEstoque() throws Exception {
        Contexto contexto = criarContexto("Justificativa", "16899535009", 10);
        long origemId = registrar(contexto, TipoMovimentacao.RETIRADA, 4);

        estornar(adminToken, origemId, "   ", "estorno-vazio")
                .andExpect(status().isBadRequest());

        assertEquals(6, estoque(contexto.material()));
        assertEquals(1, movimentacaoRepository.count());
        assertTrue(movimentacaoRepository.findById(origemId).isPresent());
    }

    @Test
    void entradaDeNotaFiscalEOutroEstornoNaoPodemSerEstornados() throws Exception {
        Contexto contexto = criarContexto("Tipos", "39053344705", 10);
        NotaFiscalEntrada nota = criarNotaFiscal();
        Movimentacao entrada = salvarMovimentacao(
                contexto,
                TipoMovimentacao.ENTRADA,
                3,
                nota,
                null
        );

        estornar(adminToken, entrada.getId(), "Correção indevida", "estorno-entrada")
                .andExpect(status().isConflict());

        long retiradaId = registrar(contexto, TipoMovimentacao.RETIRADA, 2);
        MvcResult primeiroEstorno = estornar(
                adminToken,
                retiradaId,
                "Primeiro estorno",
                "estorno-tipos"
        ).andExpect(status().isCreated()).andReturn();
        long estornoId = jsonResponse(primeiroEstorno).get("id").asLong();

        estornar(adminToken, estornoId, "Estorno do estorno", "estorno-recursivo")
                .andExpect(status().isConflict());
        estornar(adminToken, retiradaId, "Segundo estorno", "estorno-duplicado")
                .andExpect(status().isConflict());

        assertEquals(3, movimentacaoRepository.count());
        assertTrue(movimentacaoRepository.findById(entrada.getId()).isPresent());
    }

    @Test
    void estornoDeDevolucaoSemEstoqueFazRollbackCompleto() throws Exception {
        Contexto contexto = criarContexto("Sem estoque", "15350946056", 2);
        Movimentacao devolucao = salvarMovimentacao(
                contexto,
                TipoMovimentacao.DEVOLUCAO,
                4,
                null,
                null
        );

        estornar(adminToken, devolucao.getId(), "Corrigir devolução", "estorno-negativo")
                .andExpect(status().isConflict());

        assertEquals(2, estoque(contexto.material()));
        assertEquals(1, movimentacaoRepository.count());
        assertEquals(0, comprovanteRepository.count());
        Movimentacao preservada = movimentacaoRepository.findById(devolucao.getId()).orElseThrow();
        assertEquals(TipoMovimentacao.DEVOLUCAO, preservada.getTipo());
        assertEquals(4, preservada.getQuantidade());
    }

    @Test
    void encarregadoEContratoInativosNaoImpedemCorrecaoHistorica() throws Exception {
        Contexto contexto = criarContexto("Inativos", "93541134780", 10);
        long origemId = registrar(contexto, TipoMovimentacao.RETIRADA, 4);
        contexto.encarregado().setAtivo(false);
        contexto.contrato().setAtivo(false);
        encarregadoRepository.save(contexto.encarregado());
        contratoRepository.save(contexto.contrato());

        estornar(adminToken, origemId, "Correção histórica", "estorno-inativos")
                .andExpect(status().isCreated());

        assertEquals(10, estoque(contexto.material()));
    }

    @Test
    void endpointExigeAutenticacaoEPerfilAdmin() throws Exception {
        Contexto contexto = criarContexto("Seguranca", "11122233396", 10);
        Movimentacao origem = salvarMovimentacao(
                contexto,
                TipoMovimentacao.RETIRADA,
                4,
                null,
                null
        );
        contexto.material().setQuantidadeEstoque(6);
        materialRepository.save(contexto.material());

        estornar(null, origem.getId(), "Correção", "estorno-sem-auth")
                .andExpect(status().isUnauthorized());
        estornar(operadorToken, origem.getId(), "Correção", "estorno-operador")
                .andExpect(status().isForbidden());
        estornar(gerenteToken, origem.getId(), "Correção", "estorno-gerente")
                .andExpect(status().isForbidden());
        estornar(adminToken, origem.getId(), "Correção", "estorno-admin")
                .andExpect(status().isCreated());

        assertEquals(10, estoque(contexto.material()));
        assertEquals(2, movimentacaoRepository.count());
    }

    @Test
    void duasRequisicoesConcorrentesProduzemUmUnicoEstornoEfetivo()
            throws Exception {
        Contexto contexto = criarContexto("Concorrencia", "28001238938", 10);
        long origemId = registrar(contexto, TipoMovimentacao.RETIRADA, 4);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        Callable<Integer> primeiraChamada = () -> estornoConcorrente(
                origemId,
                "concorrente-1",
                prontas,
                inicio
        );
        Callable<Integer> segundaChamada = () -> estornoConcorrente(
                origemId,
                "concorrente-2",
                prontas,
                inicio
        );

        try {
            Future<Integer> primeira = executor.submit(primeiraChamada);
            Future<Integer> segunda = executor.submit(segundaChamada);
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            inicio.countDown();
            List<Integer> statusCodes = List.of(
                    primeira.get(10, TimeUnit.SECONDS),
                    segunda.get(10, TimeUnit.SECONDS)
            );
            assertEquals(1, statusCodes.stream().filter(status -> status == 201).count());
            assertEquals(1, statusCodes.stream().filter(status -> status == 409).count());
        } finally {
            executor.shutdownNow();
        }

        assertEquals(10, estoque(contexto.material()));
        assertEquals(2, movimentacaoRepository.count());
        assertEquals(1, movimentacaoRepository.findAll().stream()
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.ESTORNO_RETIRADA)
                .count());
    }

    @Test
    void retryComMesmaChaveDeIdempotenciaRetornaMesmoEstorno() throws Exception {
        Contexto contexto = criarContexto("Retry", "01234567890", 10);
        long origemId = registrar(contexto, TipoMovimentacao.RETIRADA, 4);

        long primeiroId = jsonResponse(estornar(
                adminToken,
                origemId,
                "Correção idempotente",
                "mesma-chave-estorno"
        ).andExpect(status().isCreated()).andReturn()).get("id").asLong();
        long segundoId = jsonResponse(estornar(
                adminToken,
                origemId,
                "Correção idempotente",
                "mesma-chave-estorno"
        ).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        assertEquals(primeiroId, segundoId);
        assertEquals(10, estoque(contexto.material()));
        assertEquals(2, movimentacaoRepository.count());
    }

    @Test
    void saldoPendenteConsideraOsDoisTiposDeEstorno() throws Exception {
        Contexto retiradaEstornada = criarContexto("Saldo retirada", "98765432100", 20);
        long retiradaId = registrar(retiradaEstornada, TipoMovimentacao.RETIRADA, 10);
        estornar(adminToken, retiradaId, "Cancelar retirada", "saldo-retirada")
                .andExpect(status().isCreated());
        registrarComStatus(retiradaEstornada, TipoMovimentacao.DEVOLUCAO, 1)
                .andExpect(status().isConflict());

        Contexto devolucaoEstornada = criarContexto("Saldo devolucao", "12312312387", 20);
        registrar(devolucaoEstornada, TipoMovimentacao.RETIRADA, 10);
        long devolucaoId = registrar(devolucaoEstornada, TipoMovimentacao.DEVOLUCAO, 4);
        estornar(adminToken, devolucaoId, "Cancelar devolução", "saldo-devolucao")
                .andExpect(status().isCreated());
        registrarComStatus(devolucaoEstornada, TipoMovimentacao.DEVOLUCAO, 10)
                .andExpect(status().isCreated());

        assertEquals(20, estoque(retiradaEstornada.material()));
        assertEquals(20, estoque(devolucaoEstornada.material()));
    }

    private int estornoConcorrente(
            long origemId,
            String chave,
            CountDownLatch prontas,
            CountDownLatch inicio
    ) throws Exception {
        prontas.countDown();
        inicio.await(5, TimeUnit.SECONDS);
        return estornar(adminToken, origemId, "Correção concorrente", chave)
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private org.springframework.test.web.servlet.ResultActions estornar(
            String token,
            long origemId,
            String justificativa,
            String idempotencyKey
    ) throws Exception {
        var request = post("/movimentacoes/{id}/estorno", origemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("justificativa", justificativa)))
                .header("Idempotency-Key", idempotencyKey);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return mockMvc.perform(request);
    }

    private long registrar(
            Contexto contexto,
            TipoMovimentacao tipo,
            int quantidade
    ) throws Exception {
        return jsonResponse(
                registrarComStatus(contexto, tipo, quantidade)
                        .andExpect(status().isCreated())
                        .andReturn()
        ).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions registrarComStatus(
            Contexto contexto,
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)));
    }

    private Movimentacao salvarMovimentacao(
            Contexto contexto,
            TipoMovimentacao tipo,
            int quantidade,
            NotaFiscalEntrada notaFiscal,
            Movimentacao origem
    ) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setEncarregado(tipo == TipoMovimentacao.ENTRADA ? null : contexto.encarregado());
        movimentacao.setContrato(tipo == TipoMovimentacao.ENTRADA ? null : contexto.contrato());
        movimentacao.setMaterial(contexto.material());
        movimentacao.setQuantidade(quantidade);
        movimentacao.setTipo(tipo);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setDataFinalizacao(LocalDateTime.now());
        movimentacao.setRegistradoPor(admin);
        movimentacao.setNotaFiscal(notaFiscal);
        movimentacao.setMovimentacaoOrigem(origem);
        return movimentacaoRepository.saveAndFlush(movimentacao);
    }

    private NotaFiscalEntrada criarNotaFiscal() {
        NotaFiscalEntrada nota = new NotaFiscalEntrada();
        nota.setNumero("123");
        nota.setSerie("1");
        nota.setChaveAcesso("00000000000000000000000000000000000000000001");
        nota.setFornecedor("Fornecedor");
        nota.setCnpjFornecedor("11222333000181");
        nota.setDataEmissao(LocalDate.now());
        nota.setDataEntrada(LocalDateTime.now());
        nota.setDataCadastro(LocalDateTime.now());
        nota.setStatus(StatusNotaFiscal.CONFIRMADA);
        nota.setCadastradaPor(admin);
        return notaFiscalRepository.saveAndFlush(nota);
    }

    private Contexto criarContexto(String sufixo, String cpf, int estoque) {
        return new Contexto(
                encarregadoRepository.save(TestUsuarioFactory.encarregado("Funcionário " + sufixo, cpf, "Cargo")),
                contratoRepository.save(new Contrato("Contrato " + sufixo, "Descrição", true)),
                materialRepository.save(new Material("Material " + sufixo, "Descrição", estoque))
        );
    }

    private int estoque(Material material) {
        return materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque();
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return jsonResponse(result).get("token").asText();
    }

    private JsonNode jsonResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Contexto(
            Usuario encarregado,
            Contrato contrato,
            Material material
    ) {
    }
}
