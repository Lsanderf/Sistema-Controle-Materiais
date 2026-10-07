package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SolicitacaoRetiradaPendenciaIntegrationTests {

    protected static final String CONFLITO_PENDENCIA =
            "O encarregado já possui uma solicitação de retirada aguardando assinatura.";
    private static final String SENHA = "senhaPendencia123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected MaterialRepository materialRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;
    @Autowired private UsuarioService usuarioService;

    @Test
    void primeiraSolicitacaoPendenteNaoProduzEfeitosDeConfirmacao() throws Exception {
        Cenario cenario = cenario();

        criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ASSINATURA"));

        assertSemEfeitosDeConfirmacao(cenario, 1);
    }

    @Test
    void segundaSolicitacaoDoMesmoOperadorRetorna409SemRegistroParcial() throws Exception {
        Cenario cenario = cenario();
        primeira(cenario);

        criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(CONFLITO_PENDENCIA));

        assertSemEfeitosDeConfirmacao(cenario, 1);
    }

    @Test
    void segundoOperadorTambemNaoPodeCriarPendenciaParaOMesmoEncarregado() throws Exception {
        Cenario cenario = cenario();
        primeira(cenario);

        criar(cenario, cenario.tokenOutroOperador(), cenario.contrato(), cenario.material(), "outra-chave")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(CONFLITO_PENDENCIA));

        assertSemEfeitosDeConfirmacao(cenario, 1);
    }

    @Test
    void contratoEMaterialDiferentesNaoPermitemSegundaPendencia() throws Exception {
        Cenario cenario = cenario();
        primeira(cenario);

        criar(cenario, cenario.tokenOperador(), cenario.outroContrato(), cenario.outroMaterial(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(CONFLITO_PENDENCIA));

        assertSemEfeitosDeConfirmacao(cenario, 1);
    }

    @Test
    void cancelamentoLiberaNovaSolicitacaoSemExcluirAAnterior() throws Exception {
        Cenario cenario = cenario();
        long primeiraId = primeira(cenario);
        criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), null)
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/solicitacoes-retirada/{id}/cancelar", primeiraId)
                        .header(HttpHeaders.AUTHORIZATION, cenario.tokenOperador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        long novaId = id(criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), null)
                .andExpect(status().isCreated()).andReturn());

        assertNotEquals(primeiraId, novaId);
        assertEquals("CANCELADA", statusPersistido(primeiraId));
        assertSemEfeitosDeConfirmacao(cenario, 2);
    }

    @Test
    void confirmacaoLiberaNovaSolicitacaoSemRepetirBaixaOuAssinatura() throws Exception {
        Cenario cenario = cenario();
        long primeiraId = primeira(cenario);

        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", primeiraId)
                        .file(ImagemEvidenciaTestSupport.assinatura())
                        .header(HttpHeaders.AUTHORIZATION, cenario.tokenEncarregado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
        long novaId = id(criar(cenario, cenario.tokenOutroOperador(), cenario.contrato(), cenario.material(), null)
                .andExpect(status().isCreated()).andReturn());

        assertNotEquals(primeiraId, novaId);
        assertEquals("CONFIRMADA", statusPersistido(primeiraId));
        assertEquals(2, totalSolicitacoes(cenario));
        assertEquals(1, totalPendentes(cenario));
        assertEquals(8, materialRepository.findById(cenario.material().getId()).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, movimentacaoRepository.findByMaterialId(cenario.material().getId()).size());
        assertEquals(1, totalAssinaturas(cenario));
    }

    @Test
    void retryIdempotenteDevolveAMesmaSolicitacaoAntesDaValidacaoDePendencia() throws Exception {
        Cenario cenario = cenario();
        String chave = UUID.randomUUID().toString();
        long primeiraId = id(criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), chave)
                .andExpect(status().isCreated()).andReturn());

        criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), chave)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(primeiraId));
        criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), "chave-nova")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(CONFLITO_PENDENCIA));
        MvcResult chaveReutilizada = criar(cenario, cenario.tokenOperador(), cenario.outroContrato(),
                cenario.outroMaterial(), chave).andExpect(status().isConflict()).andReturn();
        assertNotEquals(CONFLITO_PENDENCIA,
                objectMapper.readTree(chaveReutilizada.getResponse().getContentAsString()).get("erro").asText());

        assertSemEfeitosDeConfirmacao(cenario, 1);
        mockMvc.perform(patch("/solicitacoes-retirada/{id}/cancelar", primeiraId)
                        .header(HttpHeaders.AUTHORIZATION, cenario.tokenOperador()))
                .andExpect(status().isOk());
        primeira(cenario);
        criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), chave)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(primeiraId))
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        assertSemEfeitosDeConfirmacao(cenario, 2);
    }

    @Test
    void encarregadosDiferentesPodemTerPendenciasSimultaneas() throws Exception {
        Cenario primeiro = cenario();
        Cenario segundo = cenario();

        primeira(primeiro);
        primeira(segundo);

        assertEquals(1, totalPendentes(primeiro));
        assertEquals(1, totalPendentes(segundo));
    }

    protected Cenario cenario() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = usuario("op-pend-" + sufixo, Role.OPERADOR);
        Usuario outroOperador = usuario("op2-pend-" + sufixo, Role.OPERADOR);
        Usuario encarregado = usuario("enc-pend-" + sufixo, Role.ENCARREGADO);
        return new Cenario(operador, outroOperador, encarregado, token(operador), token(outroOperador), token(encarregado),
                contratoRepository.save(new Contrato("Contrato pendente " + sufixo, "Teste", true)),
                contratoRepository.save(new Contrato("Outro contrato pendente " + sufixo, "Teste", true)),
                materialRepository.save(new Material("Material pendente " + sufixo, "Teste", 10)),
                materialRepository.save(new Material("Outro material pendente " + sufixo, "Teste", 10)));
    }

    protected ResultActions criar(Cenario cenario, String token, Contrato contrato, Material material, String chave) throws Exception {
        var request = post("/solicitacoes-retirada")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "encarregadoAssinanteId", cenario.encarregado().getId(), "contratoId", contrato.getId(),
                        "itens", List.of(Map.of("materialId", material.getId(), "quantidade", 2)))));
        if (chave != null) request.header("Idempotency-Key", chave);
        return mockMvc.perform(request);
    }

    protected long primeira(Cenario cenario) throws Exception {
        return id(criar(cenario, cenario.tokenOperador(), cenario.contrato(), cenario.material(), null)
                .andExpect(status().isCreated()).andReturn());
    }

    protected long id(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    protected long totalSolicitacoes(Cenario cenario) {
        return jdbc.queryForObject("select count(*) from tb_solicitacoes_retirada where encarregado_assinante_id = ?",
                Long.class, cenario.encarregado().getId());
    }

    protected long totalPendentes(Cenario cenario) {
        return jdbc.queryForObject("select count(*) from tb_solicitacoes_retirada where encarregado_assinante_id = ? "
                + "and status = 'AGUARDANDO_ASSINATURA'", Long.class, cenario.encarregado().getId());
    }

    protected void assertSemEfeitosDeConfirmacao(Cenario cenario, long quantidadeSolicitacoes) {
        assertEquals(quantidadeSolicitacoes, totalSolicitacoes(cenario));
        assertEquals(1, totalPendentes(cenario));
        assertEquals(quantidadeSolicitacoes, jdbc.queryForObject("select count(*) from tb_solicitacoes_retirada_itens i "
                + "join tb_solicitacoes_retirada s on s.id = i.solicitacao_retirada_id "
                + "where s.encarregado_assinante_id = ?", Long.class, cenario.encarregado().getId()));
        assertEquals(0, totalAssinaturas(cenario));
        for (Material material : List.of(cenario.material(), cenario.outroMaterial())) {
            assertEquals(10, materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
            assertTrue(movimentacaoRepository.findByMaterialId(material.getId()).isEmpty());
        }
    }

    private long totalAssinaturas(Cenario cenario) {
        return jdbc.queryForObject("select count(*) from tb_assinaturas_retirada a "
                + "join tb_solicitacoes_retirada s on s.id = a.solicitacao_retirada_id "
                + "where s.encarregado_assinante_id = ?", Long.class, cenario.encarregado().getId());
    }

    private String statusPersistido(long id) {
        return jdbc.queryForObject("select status from tb_solicitacoes_retirada where id = ?", String.class, id);
    }

    private String token(Usuario usuario) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", usuario.getUsername(), "password", SENHA))))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Usuario usuario(String username, Role role) {
        String cpf;
        do {
            cpf = TestUsuarioFactory.proximoCpf();
        } while (jdbc.queryForObject("select count(*) from tb_usuarios where cpf = ?", Long.class, cpf) > 0);
        return usuarioService.criarUsuario("Usuário " + username, cpf, "11999999999", username, SENHA, role, true);
    }

    protected record Cenario(Usuario operador, Usuario outroOperador, Usuario encarregado,
                             String tokenOperador, String tokenOutroOperador, String tokenEncarregado,
                             Contrato contrato, Contrato outroContrato, Material material, Material outroMaterial) { }
}
