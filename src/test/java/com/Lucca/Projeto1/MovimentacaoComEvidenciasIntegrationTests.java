package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.*;
import com.Lucca.Projeto1.repository.*;
import com.Lucca.Projeto1.service.UsuarioService;
import com.Lucca.Projeto1.storage.EvidenciaStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.evidencias.diretorio=target/test-evidencias-obrigatorias")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MovimentacaoComEvidenciasIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsuarioService usuarios;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private FuncionarioRepository funcionarios;
    @Autowired private ContratoRepository contratos;
    @Autowired private MaterialRepository materiais;
    @Autowired private MovimentacaoRepository movimentos;
    @Autowired private NotaFiscalEntradaRepository notas;
    @MockitoSpyBean private EvidenciaMovimentacaoRepository evidencias;
    @MockitoSpyBean private ComprovanteMovimentacaoRepository comprovantes;
    @MockitoSpyBean private EvidenciaStorage storage;

    private Material material;
    private Funcionario funcionario;
    private Contrato contrato;
    private String operador;
    private String gerente;
    private String admin;

    @BeforeEach
    void preparar() throws Exception {
        evidencias.deleteAll();
        comprovantes.deleteAll();
        movimentos.deleteAll();
        notas.deleteAll();
        funcionarios.deleteAll();
        contratos.deleteAll();
        materiais.deleteAll();
        usuarioRepository.deleteAll();
        TestUsuarioFactory.criarUsuario(
                usuarios, "operador", "senhaTeste123", Role.OPERADOR, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarios, "gerente", "senhaTeste123", Role.GERENTE, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarios, "admin", "senhaTeste123", Role.ADMIN, true
        );
        operador = token("operador");
        gerente = token("gerente");
        admin = token("admin");
        funcionario = funcionarios.save(new Funcionario("João", "12345678909", "Técnico"));
        contrato = contratos.save(new Contrato("Contrato A", "Obra", true));
        material = materiais.save(new Material("Cabo", "Cabo óptico", 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RETIRADA", "DEVOLUCAO"})
    void assinaturaObrigatoriaConcluiComEstoqueEComprovante(String tipo) throws Exception {
        if (tipo.equals("DEVOLUCAO")) historica();
        MvcResult result = mvc.perform(requisicao(tipo).file(assinatura())
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isCreated()).andReturn();
        long id = json(result).get("id").asLong();
        assertEquals(tipo.equals("RETIRADA") ? 7 : 13, estoque());
        mvc.perform(get("/movimentacoes/{id}/comprovante", id).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty())
                .andExpect(jsonPath("$.evidencias.length()").value(1))
                .andExpect(jsonPath("$.evidencias[0].tipo").value("ASSINATURA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RETIRADA", "DEVOLUCAO"})
    void parteAusenteEJsonAntigoNaoPermitemBypass(String tipo) throws Exception {
        if (tipo.equals("DEVOLUCAO")) historica();
        Estado antes = estado();
        mvc.perform(requisicao(tipo).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isBadRequest());
        assertEquals(antes, estado());
        mvc.perform(post("/movimentacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(tipo)).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("O arquivo da assinatura é obrigatório"));
        assertEquals(antes, estado());
    }

    @Test
    void devolucaoComFotoJpegEAssinaturaPngUsaStorageSemExporChaves() throws Exception {
        historica();
        byte[] jpeg = imagem("jpg", true);
        MvcResult result = mvc.perform(requisicao("DEVOLUCAO").file(assinatura())
                        .file(new MockMultipartFile("foto", "../../material.jpg", "image/jpeg", jpeg))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isCreated()).andReturn();
        long id = json(result).get("id").asLong();
        assertEquals(13, estoque());
        MvcResult receipt = mvc.perform(get("/movimentacoes/{id}/comprovante", id)
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidencias.length()").value(2))
                .andExpect(jsonPath("$.evidencias[1].tipo").value("FOTO_DEVOLUCAO"))
                .andExpect(jsonPath("$.evidencias[1].nomeArquivo").value("material.jpg"))
                .andExpect(jsonPath("$.evidencias[1].storageKey").doesNotExist()).andReturn();
        String url = json(receipt).get("evidencias").get(1).get("urlArquivo").asText();
        mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isOk()).andExpect(content().bytes(jpeg));
        var foto = evidencias.findByMovimentacaoIdOrderByDataEvidenciaAsc(id).get(1);
        assertFalse(foto.getStorageKey().contains("material.jpg"));
        assertEquals(64, foto.getSha256().length());
        Estado antes = estado();
        mvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(multipart("/movimentacoes/{id}/assinatura", id)
                        .file(new MockMultipartFile("arquivo", "nova.png", "image/png", imagem("png", true)))
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isConflict());
        assertEquals(antes, estado());
    }

    @Test
    void retiradaNaoAceitaFotoNemEntradaManual() throws Exception {
        Estado antes = estado();
        for (String tipo : List.of("RETIRADA", "ENTRADA")) {
            mvc.perform(requisicao(tipo).file(assinatura()).file(foto())
                            .header(HttpHeaders.AUTHORIZATION, operador))
                    .andExpect(status().isConflict());
            assertEquals(antes, estado());
        }
    }

    @Test
    void assinaturaVaziaEmBrancoTruncadaMimeFalsoETamanhoExcessivoSaoRejeitados() throws Exception {
        Estado antes = estado();
        List<MockMultipartFile> invalidas = List.of(
                new MockMultipartFile("assinatura", "vazia.png", "image/png", new byte[0]),
                new MockMultipartFile("assinatura", "branco.png", "image/png", imagem("png", false)),
                new MockMultipartFile("assinatura", "falsa.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10}),
                new MockMultipartFile("assinatura", "mime.png", "image/jpeg", imagem("png", true)),
                new MockMultipartFile("assinatura", "mime.png", "text/plain", imagem("png", true)),
                new MockMultipartFile("assinatura", "grande.png", "image/png", new byte[2 * 1024 * 1024 + 1])
        );
        for (MockMultipartFile invalida : invalidas) {
            mvc.perform(requisicao("RETIRADA").file(invalida).header(HttpHeaders.AUTHORIZATION, operador))
                    .andExpect(status().isConflict());
            assertEquals(antes, estado());
        }
    }

    @Test
    void assinaturaJpegValidaTambemEPermitida() throws Exception {
        mvc.perform(requisicao("RETIRADA")
                        .file(new MockMultipartFile("assinatura", "assinatura.jpg", "image/jpeg", imagem("jpg", true)))
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isCreated());
        assertEquals(7, estoque());
    }

    @Test
    void fotoInvalidaNaoCriaNadaEPodeSerOmitidaNaNovaTentativa() throws Exception {
        historica();
        Estado antes = estado();
        mvc.perform(requisicao("DEVOLUCAO").file(assinatura())
                        .file(new MockMultipartFile("foto", "foto.png", "image/png", new byte[]{1, 2, 3}))
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isConflict());
        assertEquals(antes, estado());
        mvc.perform(requisicao("DEVOLUCAO").file(assinatura()).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isCreated());
        assertEquals(13, estoque());
    }

    @Test
    void gerenteNaoPodeCriarMesmoEnviandoTodasAsEvidencias() throws Exception {
        Estado antes = estado();
        mvc.perform(requisicao("DEVOLUCAO").file(assinatura()).file(foto())
                        .header(HttpHeaders.AUTHORIZATION, gerente))
                .andExpect(status().isForbidden());
        assertEquals(antes, estado());
    }

    @Test
    void historicoSemAssinaturaContinuaConsultavelSemInventarEvidencias() throws Exception {
        long id = historica();
        Estado antes = estado();
        mvc.perform(get("/movimentacoes/{id}/comprovante", id).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evidencias").isEmpty());
        assertEquals(antes, estado());
    }

    @Test
    void falhaDoStorageDepoisDeEscreverCompensaArquivoEReverteEstoque() throws Exception {
        Estado antes = estado();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("Falha de storage"); })
                .when(storage).armazenar(anyString(), any(byte[].class));
        mvc.perform(requisicao("RETIRADA").file(assinatura()).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isInternalServerError());
        assertEquals(antes, estado());
    }

    @Test
    void falhaNaFotoReverteTambemAssinaturaEMovimentacao() throws Exception {
        historica();
        Estado antes = estado();
        doThrow(new IllegalStateException("Falha de foto"))
                .when(storage).armazenar(contains("/foto_devolucao/"), any(byte[].class));
        mvc.perform(requisicao("DEVOLUCAO").file(assinatura()).file(foto())
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isInternalServerError());
        assertEquals(antes, estado());
    }

    @Test
    void falhaNosMetadadosNaoDeixaArquivoOuEstoqueAlterado() throws Exception {
        Estado antes = estado();
        doThrow(new DataIntegrityViolationException("Falha de metadados"))
                .when(evidencias).saveAndFlush(any());
        mvc.perform(requisicao("RETIRADA").file(assinatura()).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isConflict());
        assertEquals(antes, estado());
    }

    @Test
    void falhaNoComprovanteReverteMovimentacaoEstoqueEArquivos() throws Exception {
        Estado antes = estado();
        doThrow(new IllegalStateException("Falha de comprovante")).when(comprovantes).save(any());
        mvc.perform(requisicao("RETIRADA").file(assinatura()).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isInternalServerError());
        assertEquals(antes, estado());
    }

    @Test
    void falhaTardiaNoCommitRemoveAmbosArquivosEReverteBanco() throws Exception {
        historica();
        Estado antes = estado();
        doAnswer(call -> {
            Object result = call.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("Falha ao confirmar transação");
                }
            });
            return result;
        }).when(comprovantes).save(any());
        mvc.perform(requisicao("DEVOLUCAO").file(assinatura()).file(foto())
                        .header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isInternalServerError());
        assertEquals(antes, estado());
    }

    private MockMultipartFile foto() throws Exception {
        return new MockMultipartFile("foto", "foto.png", "image/png", imagem("png", true));
    }

    private MockMultipartHttpServletRequestBuilder requisicao(String tipo) throws Exception {
        return multipart("/movimentacoes").file(dados(payload(tipo)));
    }

    private String payload(String tipo) throws Exception {
        return mapper.writeValueAsString(Map.of("funcionarioId", funcionario.getId(),
                "contratoId", contrato.getId(), "materialId", material.getId(), "quantidade", 3, "tipo", tipo));
    }

    private long historica() {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setFuncionario(funcionario);
        movimentacao.setContrato(contrato);
        movimentacao.setMaterial(material);
        movimentacao.setTipo(TipoMovimentacao.RETIRADA);
        movimentacao.setQuantidade(6);
        movimentacao = movimentos.saveAndFlush(movimentacao);
        comprovantes.saveAndFlush(ComprovanteMovimentacao.registrar(movimentacao));
        return movimentacao.getId();
    }

    private int estoque() { return materiais.findById(material.getId()).orElseThrow().getQuantidadeEstoque(); }

    private Estado estado() throws Exception {
        Path raiz = Path.of("target/test-evidencias-obrigatorias");
        Set<String> arquivos = Set.of();
        if (Files.exists(raiz)) {
            try (var stream = Files.walk(raiz)) {
                arquivos = stream.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toSet());
            }
        }
        return new Estado(estoque(), movimentos.count(), comprovantes.count(), evidencias.count(), arquivos);
    }

    private String token(String username) throws Exception {
        return "Bearer " + json(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", "senhaTeste123"))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private record Estado(int estoque, long movimentos, long comprovantes, long evidencias, Set<String> arquivos) { }
}
