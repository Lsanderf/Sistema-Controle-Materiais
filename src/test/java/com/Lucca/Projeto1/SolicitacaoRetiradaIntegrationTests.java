package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.AssinaturaRetiradaRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.model.TipoEvidenciaMovimentacao;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SolicitacaoRetiradaIntegrationTests {

    private static final String SENHA = "senhaSolicitacao123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;
    @Autowired private AssinaturaRetiradaRepository assinaturaRetiradaRepository;
    @Autowired private EvidenciaMovimentacaoRepository evidenciaMovimentacaoRepository;

    @Test
    void operadorCriaSolicitacaoComEstoqueSemBaixarMaterial() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(usuarioService, "operador-sol-" + sufixo, SENHA, Role.OPERADOR, true);
        Usuario encarregado = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-sol-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Contrato contrato = contratoRepository.save(new Contrato("Contrato solicitaÃ§Ã£o " + sufixo, "Teste", true));
        Material material = materialRepository.save(new Material("Material solicitaÃ§Ã£o " + sufixo, "Teste", 5));

        mockMvc.perform(post("/solicitacoes-retirada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(operador.getUsername())))
                        .header("Idempotency-Key", "solicitacao-" + sufixo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoAssinanteId", encarregado.getId(),
                                "contratoId", contrato.getId(),
                                "itens", List.of(Map.of("materialId", material.getId(), "quantidade", 3))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ASSINATURA"))
                .andExpect(jsonPath("$.operadorResponsavel.id").value(operador.getId()))
                .andExpect(jsonPath("$.encarregadoAssinante.id").value(encarregado.getId()))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3));

        assertEquals(5, materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void faltaDeEstoqueImpedeCriacaoEInformaDisponivelESolicitado() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(usuarioService, "operador-falta-" + sufixo, SENHA, Role.OPERADOR, true);
        Usuario encarregado = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-falta-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Contrato contrato = contratoRepository.save(new Contrato("Contrato falta " + sufixo, "Teste", true));
        Material material = materialRepository.save(new Material("Material falta " + sufixo, "Teste", 2));

        mockMvc.perform(post("/solicitacoes-retirada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(operador.getUsername())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoAssinanteId", encarregado.getId(),
                                "contratoId", contrato.getId(),
                                "itens", List.of(Map.of("materialId", material.getId(), "quantidade", 5))
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTOQUE_INSUFICIENTE"))
                .andExpect(jsonPath("$.itens[0].disponivel").value(2))
                .andExpect(jsonPath("$.itens[0].solicitado").value(5))
                .andExpect(jsonPath("$.itens[0].faltante").value(3));

        assertEquals(2, materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void encarregadoDestinatarioConfirmaUmaVezEAtomizaBaixasDeTodosOsItens() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(usuarioService, "operador-conf-" + sufixo, SENHA, Role.OPERADOR, true);
        Usuario encarregado = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-conf-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Contrato contrato = contratoRepository.save(new Contrato("Contrato confirmaÃ§Ã£o " + sufixo, "Teste", true));
        Material primeiro = materialRepository.save(new Material("Material confirmaÃ§Ã£o A " + sufixo, "Teste", 5));
        Material segundo = materialRepository.save(new Material("Material confirmaÃ§Ã£o B " + sufixo, "Teste", 7));
        long solicitacaoId = criarSolicitacao(operador, encarregado, contrato, List.of(
                Map.of("materialId", primeiro.getId(), "quantidade", 3),
                Map.of("materialId", segundo.getId(), "quantidade", 4)
        ));

        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                        .file(ImagemEvidenciaTestSupport.assinatura())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(encarregado.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));

        assertEquals(2, movimentacaoRepository.findByMaterialId(primeiro.getId()).size()
                + movimentacaoRepository.findByMaterialId(segundo.getId()).size());
        assertTrue(assinaturaRetiradaRepository.existsBySolicitacaoRetiradaId(solicitacaoId));
        assertTrue(movimentacaoRepository.findByMaterialId(primeiro.getId()).stream()
                .allMatch(movimentacao -> movimentacao.getSolicitacaoRetirada().getId().equals(solicitacaoId)
                        && movimentacao.getRegistradoPor().getId().equals(operador.getId())
                        && movimentacao.getEncarregadoAssinante().getId().equals(encarregado.getId())));
        assertEquals(2, materialRepository.findById(primeiro.getId()).orElseThrow().getQuantidadeEstoque());
        assertEquals(3, materialRepository.findById(segundo.getId()).orElseThrow().getQuantidadeEstoque());

        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                        .file(ImagemEvidenciaTestSupport.assinatura())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(encarregado.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));

        assertEquals(2, movimentacaoRepository.findByMaterialId(primeiro.getId()).size()
                + movimentacaoRepository.findByMaterialId(segundo.getId()).size());
    }

    @Test
    void comprovanteDaRetiradaConfirmadaReferenciaAssinaturaDaSolicitacaoComArquivoProtegido() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(
                usuarioService, "operador-comprovante-" + sufixo, SENHA, Role.OPERADOR, true
        );
        Usuario encarregado = TestUsuarioFactory.criarUsuario(
                usuarioService, "encarregado-comprovante-" + sufixo, SENHA, Role.ENCARREGADO, true
        );
        Contrato contrato = contratoRepository.save(
                new Contrato("Contrato comprovante " + sufixo, "Teste", true)
        );
        Material material = materialRepository.save(
                new Material("Material comprovante " + sufixo, "Teste", 5)
        );
        long solicitacaoId = criarSolicitacao(
                operador,
                encarregado,
                contrato,
                List.of(Map.of("materialId", material.getId(), "quantidade", 2))
        );
        byte[] assinatura = ImagemEvidenciaTestSupport.imagem("png", true);

        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "assinatura", "assinatura.png", MediaType.IMAGE_PNG_VALUE, assinatura
                        ))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(encarregado.getUsername()))))
                .andExpect(status().isOk());

        long movimentacaoId = movimentacaoRepository.findByMaterialId(material.getId())
                .getFirst()
                .getId();
        String operadorToken = token(operador.getUsername());
        String encarregadoToken = token(encarregado.getUsername());
        String urlArquivo = "/movimentacoes/" + movimentacaoId + "/assinatura-retirada/arquivo";

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitacaoRetiradaId").value(solicitacaoId))
                .andExpect(jsonPath("$.operadorResponsavel.id").value(operador.getId()))
                .andExpect(jsonPath("$.operadorResponsavel.nome").value(operador.getNome()))
                .andExpect(jsonPath("$.encarregadoAssinante.id").value(encarregado.getId()))
                .andExpect(jsonPath("$.encarregadoAssinante.nome").value(encarregado.getNome()))
                .andExpect(jsonPath("$.assinaturaRetirada.encarregadoAssinante.id").value(encarregado.getId()))
                .andExpect(jsonPath("$.assinaturaRetirada.encarregadoAssinante.nome").value(encarregado.getNome()))
                .andExpect(jsonPath("$.assinaturaRetirada.dataAssinatura").isNotEmpty())
                .andExpect(jsonPath("$.assinaturaRetirada.contentType").value(MediaType.IMAGE_PNG_VALUE))
                .andExpect(jsonPath("$.assinaturaRetirada.tamanhoBytes").value(assinatura.length))
                .andExpect(jsonPath("$.assinaturaRetirada.sha256").isNotEmpty())
                .andExpect(jsonPath("$.assinaturaRetirada.urlArquivo").value(urlArquivo))
                .andExpect(jsonPath("$.assinaturaRetirada.storageKey").doesNotExist());

        mockMvc.perform(get(urlArquivo)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(assinatura))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        mockMvc.perform(get(urlArquivo)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(urlArquivo)
                        .header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/movimentacoes/{id}/assinatura", movimentacaoId)
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "arquivo", "segunda-assinatura.png", MediaType.IMAGE_PNG_VALUE, assinatura
                        ))
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken)))
                .andExpect(status().isConflict());
        assertFalse(evidenciaMovimentacaoRepository.existsByMovimentacaoIdAndTipo(
                movimentacaoId, TipoEvidenciaMovimentacao.ASSINATURA
        ));
    }

    @Test
    void outroEncarregadoNaoConfirmaESemEstoqueNaAssinaturaNenhumaBaixaOcorre() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(usuarioService, "operador-seg-" + sufixo, SENHA, Role.OPERADOR, true);
        Usuario encarregado = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-seg-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Usuario outro = TestUsuarioFactory.criarUsuario(usuarioService, "outro-seg-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Contrato contrato = contratoRepository.save(new Contrato("Contrato seguranca " + sufixo, "Teste", true));
        Material material = materialRepository.save(new Material("Material seguranca " + sufixo, "Teste", 5));
        long solicitacaoId = criarSolicitacao(operador, encarregado, contrato,
                List.of(Map.of("materialId", material.getId(), "quantidade", 4)));

        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                        .file(ImagemEvidenciaTestSupport.assinatura())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(outro.getUsername()))))
                .andExpect(status().isForbidden());
        assertFalse(assinaturaRetiradaRepository.existsBySolicitacaoRetiradaId(solicitacaoId));

        material.setQuantidadeEstoque(2);
        materialRepository.saveAndFlush(material);
        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                        .file(ImagemEvidenciaTestSupport.assinatura())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(encarregado.getUsername()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTOQUE_INSUFICIENTE"));

        assertEquals(2, materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
        assertEquals(0, movimentacaoRepository.findByMaterialId(material.getId()).size());
        assertFalse(assinaturaRetiradaRepository.existsBySolicitacaoRetiradaId(solicitacaoId));
    }

    @Test
    void operadorCancelaSolicitacaoPendenteESomenteDestinatarioAVisualiza() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(usuarioService, "operador-cancela-" + sufixo, SENHA, Role.OPERADOR, true);
        Usuario encarregado = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-cancela-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Usuario outroEncarregado = TestUsuarioFactory.criarUsuario(usuarioService, "outro-cancela-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Contrato contrato = contratoRepository.save(new Contrato("Contrato cancela " + sufixo, "Teste", true));
        Material material = materialRepository.save(new Material("Material cancela " + sufixo, "Teste", 5));
        long solicitacaoId = criarSolicitacao(operador, encarregado, contrato, List.of(Map.of("materialId", material.getId(), "quantidade", 2)));

        mockMvc.perform(get("/solicitacoes-retirada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(encarregado.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(solicitacaoId));
        mockMvc.perform(get("/solicitacoes-retirada/{id}", solicitacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(outroEncarregado.getUsername()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/solicitacoes-retirada/{id}/cancelar", solicitacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(operador.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        assertEquals(5, materialRepository.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void rolesAtuaisBloqueiamCriacaoEConfirmacaoIndevidas() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(
                usuarioService, "operador-aut-" + sufixo, SENHA, Role.OPERADOR, true
        );
        Usuario encarregado = TestUsuarioFactory.criarUsuario(
                usuarioService, "encarregado-aut-" + sufixo, SENHA, Role.ENCARREGADO, true
        );
        Usuario gerente = TestUsuarioFactory.criarUsuario(
                usuarioService, "gerente-aut-" + sufixo, SENHA, Role.GERENTE, true
        );
        Contrato contrato = contratoRepository.save(new Contrato("Contrato autorizacao " + sufixo, "Teste", true));
        Material material = materialRepository.save(new Material("Material autorizacao " + sufixo, "Teste", 5));
        Map<String, Object> solicitacao = Map.of(
                "encarregadoAssinanteId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "itens", List.of(Map.of("materialId", material.getId(), "quantidade", 2))
        );

        for (Usuario semPermissao : List.of(gerente, encarregado)) {
            mockMvc.perform(post("/solicitacoes-retirada")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token(semPermissao.getUsername())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(solicitacao)))
                    .andExpect(status().isForbidden());
        }

        long solicitacaoId = criarSolicitacao(
                operador,
                encarregado,
                contrato,
                List.of(Map.of("materialId", material.getId(), "quantidade", 2))
        );
        for (Usuario semPermissao : List.of(gerente, operador)) {
            mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                            .file(ImagemEvidenciaTestSupport.assinatura())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token(semPermissao.getUsername()))))
                    .andExpect(status().isForbidden());
        }

        Map<String, Object> devolucao = Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 1,
                "tipo", "DEVOLUCAO"
        );
        for (Usuario semPermissao : List.of(gerente, encarregado)) {
            mockMvc.perform(multipart("/movimentacoes")
                            .file(ImagemEvidenciaTestSupport.dados(json(devolucao)))
                            .file(ImagemEvidenciaTestSupport.assinatura())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token(semPermissao.getUsername()))))
                    .andExpect(status().isForbidden());
        }
    }

    private long criarSolicitacao(Usuario operador, Usuario encarregado, Contrato contrato, List<Map<String, Object>> itens) throws Exception {
        MvcResult result = mockMvc.perform(post("/solicitacoes-retirada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(operador.getUsername())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoAssinanteId", encarregado.getId(),
                                "contratoId", contrato.getId(),
                                "itens", itens
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
