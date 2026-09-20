package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.evidencias.diretorio=target/test-evidencias-rn01")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MovimentacaoInativosIntegrationTests {

    private static final String SENHA = "senhaTeste123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;
    @Autowired private EvidenciaMovimentacaoRepository evidenciaRepository;
    @Autowired private ComprovanteMovimentacaoRepository comprovanteRepository;
    @Autowired private NotaFiscalEntradaRepository notaFiscalRepository;

    private Funcionario funcionario;
    private Contrato contrato;
    private Material material;
    private String operadorToken;

    @BeforeEach
    void preparar() throws Exception {
        limparBanco();
        usuarioService.criarUsuario("operador-rn01", SENHA, Role.OPERADOR, true);
        operadorToken = token("operador-rn01");
        funcionario = funcionarioRepository.save(
                new Funcionario("Funcionário RN-01", "52998224725", "Técnico")
        );
        contrato = contratoRepository.save(
                new Contrato("Contrato RN-01", "Teste de devolução histórica", true)
        );
        material = materialRepository.save(
                new Material("Material RN-01", "Material de teste", 20)
        );
    }

    @AfterEach
    void limparBanco() {
        evidenciaRepository.deleteAllInBatch();
        comprovanteRepository.deleteAllInBatch();
        movimentacaoRepository.deleteAllInBatch();
        notaFiscalRepository.deleteAllInBatch();
        funcionarioRepository.deleteAllInBatch();
        contratoRepository.deleteAllInBatch();
        materialRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void retiradaComFuncionarioEContratoAtivosTemSucesso() throws Exception {
        registrar(TipoMovimentacao.RETIRADA, 6, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioUsername").value("operador-rn01"));

        assertEquals(14, estoque());
        assertEquals(1, movimentacaoRepository.count());
    }

    @Test
    void retiradaComFuncionarioInativoFalha() throws Exception {
        inativarFuncionario();

        registrar(TipoMovimentacao.RETIRADA, 1, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Não é possível registrar retirada para um funcionário inativo"
                ));

        assertEquals(20, estoque());
        assertEquals(0, movimentacaoRepository.count());
    }

    @Test
    void retiradaComContratoInativoFalha() throws Exception {
        inativarContrato();

        registrar(TipoMovimentacao.RETIRADA, 1, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Não é possível registrar retirada em um contrato inativo"
                ));

        assertEquals(20, estoque());
        assertEquals(0, movimentacaoRepository.count());
    }

    @Test
    void devolucaoComFuncionarioInativoTemSucessoEAtualizaEstoque() throws Exception {
        registrar(TipoMovimentacao.RETIRADA, 10, null)
                .andExpect(status().isCreated());
        inativarFuncionario();

        registrar(TipoMovimentacao.DEVOLUCAO, 4, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DEVOLUCAO"));

        assertEquals(14, estoque());
        assertEquals(2, movimentacaoRepository.count());
    }

    @Test
    void devolucaoComContratoInativoTemSucessoEAtualizaEstoque() throws Exception {
        registrar(TipoMovimentacao.RETIRADA, 10, null)
                .andExpect(status().isCreated());
        inativarContrato();

        registrar(TipoMovimentacao.DEVOLUCAO, 4, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DEVOLUCAO"));

        assertEquals(14, estoque());
        assertEquals(2, movimentacaoRepository.count());
    }

    @Test
    void devolucaoComFuncionarioEContratoInativosTemSucesso() throws Exception {
        registrar(TipoMovimentacao.RETIRADA, 10, null)
                .andExpect(status().isCreated());
        inativarFuncionario();
        inativarContrato();

        registrar(TipoMovimentacao.DEVOLUCAO, 10, null)
                .andExpect(status().isCreated());

        assertEquals(20, estoque());
        assertEquals(2, movimentacaoRepository.count());
    }

    @Test
    void devolucaoAcimaDoSaldoPendenteContinuaFalhandoComVinculosInativos()
            throws Exception {
        registrar(TipoMovimentacao.RETIRADA, 10, null)
                .andExpect(status().isCreated());
        inativarFuncionario();
        inativarContrato();

        registrar(TipoMovimentacao.DEVOLUCAO, 11, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "A devolução não pode ser maior que a quantidade ainda retirada"
                ));

        assertEquals(10, estoque());
        assertEquals(1, movimentacaoRepository.count());
    }

    @Test
    void devolucaoSemSaldoPendenteContinuaFalhandoComVinculosInativos()
            throws Exception {
        inativarFuncionario();
        inativarContrato();

        registrar(TipoMovimentacao.DEVOLUCAO, 1, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "A devolução não pode ser maior que a quantidade ainda retirada"
                ));

        assertEquals(20, estoque());
        assertEquals(0, movimentacaoRepository.count());
    }

    @Test
    void devolucaoComFuncionarioInativoContinuaIdempotente() throws Exception {
        registrar(TipoMovimentacao.RETIRADA, 8, null)
                .andExpect(status().isCreated());
        inativarFuncionario();

        MvcResult primeira = registrar(
                TipoMovimentacao.DEVOLUCAO,
                3,
                "devolucao-inativo-rn01"
        ).andExpect(status().isCreated()).andReturn();
        MvcResult repetida = registrar(
                TipoMovimentacao.DEVOLUCAO,
                3,
                "devolucao-inativo-rn01"
        ).andExpect(status().isCreated()).andReturn();

        assertEquals(json(primeira).get("id").asLong(), json(repetida).get("id").asLong());
        assertEquals(15, estoque());
        assertEquals(2, movimentacaoRepository.count());
        assertEquals(2, evidenciaRepository.count());
        assertEquals(2, comprovanteRepository.count());
    }

    private ResultActions registrar(
            TipoMovimentacao tipo,
            int quantidade,
            String idempotencyKey
    ) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "funcionarioId", funcionario.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", quantidade,
                "tipo", tipo.name()
        ));
        MockMultipartHttpServletRequestBuilder request = movimentacaoAssinada(payload)
                .header(HttpHeaders.AUTHORIZATION, operadorToken);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request);
    }

    private void inativarFuncionario() {
        funcionario.setAtivo(false);
        funcionario.setDataInativacao(LocalDateTime.now());
        funcionario = funcionarioRepository.saveAndFlush(funcionario);
    }

    private void inativarContrato() {
        contrato.setAtivo(false);
        contrato.setDataInativacao(LocalDateTime.now());
        contrato = contratoRepository.saveAndFlush(contrato);
    }

    private int estoque() {
        return materialRepository.findById(material.getId())
                .orElseThrow()
                .getQuantidadeEstoque();
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", SENHA
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + json(result).get("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
