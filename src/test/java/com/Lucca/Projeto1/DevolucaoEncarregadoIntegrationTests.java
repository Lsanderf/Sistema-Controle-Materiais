package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.service.ComprovanteMovimentacaoService;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:devolucao_encarregado_test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevolucaoEncarregadoIntegrationTests {

    private static final String SENHA = "senhaDevolucao123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ContratoRepository contratoRepository;
    @MockitoSpyBean private FuncionarioRepository funcionarioRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;
    @Autowired private ComprovanteMovimentacaoService comprovanteService;

    @Test
    void devolucaoSemFotoNaoCriaMovimentacaoNemAumentaEstoque() throws Exception {
        Contexto contexto = criarContexto(10);
        confirmarRetirada(contexto, 5);
        clearInvocations(funcionarioRepository);

        registrarDevolucao(contexto, contexto.encarregado(), 2, ImagemEvidenciaTestSupport.imagem("png", true), false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("O arquivo da foto é obrigatório"));

        verify(funcionarioRepository, never()).findById(anyLong());
        assertEquals(1, movimentacaoRepository.findByMaterialId(contexto.material().getId()).size());
        assertEquals(5, materialRepository.findById(contexto.material().getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void devolucaoNovaUsaUsuarioEncarregadoSemDependerDeFuncionarioEComprovaAIntegridadeDaAssinatura()
            throws Exception {
        Contexto contexto = criarContexto(10);
        confirmarRetirada(contexto, 5);
        byte[] assinatura = ImagemEvidenciaTestSupport.imagem("png", true);
        clearInvocations(funcionarioRepository);

        MvcResult resultado = registrarDevolucao(
                contexto,
                contexto.encarregado(),
                2,
                assinatura
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DEVOLUCAO"))
                .andExpect(jsonPath("$.encarregadoId").value(contexto.encarregado().getId()))
                .andExpect(jsonPath("$.encarregado").value(contexto.encarregado().getNome()))
                .andExpect(jsonPath("$.funcionario").doesNotExist())
                .andReturn();
        verify(funcionarioRepository, never()).findById(anyLong());

        long movimentacaoId = jsonResponse(resultado).get("id").asLong();
        Movimentacao devolucao = movimentacaoRepository.findById(movimentacaoId).orElseThrow();
        assertNull(devolucao.getFuncionario());
        assertEquals(contexto.encarregado().getId(), devolucao.getEncarregadoAssinante().getId());
        assertEquals(7, materialRepository.findById(contexto.material().getId()).orElseThrow().getQuantidadeEstoque());

        MvcResult comprovante = mockMvc.perform(get("/movimentacoes/{id}/comprovante", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionario").doesNotExist())
                .andExpect(jsonPath("$.encarregadoAssinante.id").value(contexto.encarregado().getId()))
                .andExpect(jsonPath("$.encarregadoAssinante.nome").value(contexto.encarregado().getNome()))
                .andExpect(jsonPath("$.evidencias[0].funcionario").doesNotExist())
                .andExpect(jsonPath("$.evidencias[0].encarregado.id").value(contexto.encarregado().getId()))
                .andExpect(jsonPath("$.evidencias[0].encarregado.nome").value(contexto.encarregado().getNome()))
                .andExpect(jsonPath("$.evidencias[1].tipo").value("FOTO_DEVOLUCAO"))
                .andExpect(jsonPath("$.evidencias[1].encarregado.id").value(contexto.encarregado().getId()))
                .andReturn();

        String urlArquivo = jsonResponse(comprovante).at("/evidencias/0/urlArquivo").asText();
        mockMvc.perform(get(urlArquivo)
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(assinatura));
        String urlFoto = jsonResponse(comprovante).at("/evidencias/1/urlArquivo").asText();
        mockMvc.perform(get(urlFoto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(ImagemEvidenciaTestSupport.imagem("jpg", true)));
    }

    @Test
    void saldoDaDevolucaoUsaSomenteMovimentosDoEncarregadoCorretoEImpedeExcesso()
            throws Exception {
        Contexto contexto = criarContexto(10);
        Usuario outroEncarregado = TestUsuarioFactory.criarUsuario(
                usuarioService,
                "outro-encarregado-" + contexto.sufixo(),
                SENHA,
                Role.ENCARREGADO,
                true
        );
        confirmarRetirada(contexto, 5);

        Contrato outroContrato = contratoRepository.save(new Contrato("Outro contrato " + contexto.sufixo(), "Teste", true));
        Material outroMaterial = materialRepository.save(new Material("Outro material " + contexto.sufixo(), "Teste", 10));
        for (Map<String, Object> request : List.of(
                Map.<String, Object>of("encarregadoId", contexto.encarregado().getId(), "contratoId", outroContrato.getId(),
                        "materialId", contexto.material().getId(), "quantidade", 1, "tipo", "DEVOLUCAO"),
                Map.<String, Object>of("encarregadoId", contexto.encarregado().getId(), "contratoId", contexto.contrato().getId(),
                        "materialId", outroMaterial.getId(), "quantidade", 1, "tipo", "DEVOLUCAO")
        )) {
            mockMvc.perform(multipart("/movimentacoes")
                            .file(ImagemEvidenciaTestSupport.dados(json(request)))
                            .file(ImagemEvidenciaTestSupport.assinatura())
                            .file(ImagemEvidenciaTestSupport.foto())
                            .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value(containsString("quantidade ainda retirada")));
        }

        registrarDevolucao(contexto, outroEncarregado, 1, ImagemEvidenciaTestSupport.imagem("png", true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("quantidade ainda retirada")));

        registrarDevolucao(contexto, contexto.encarregado(), 2, ImagemEvidenciaTestSupport.imagem("png", true))
                .andExpect(status().isCreated());
        registrarDevolucao(contexto, contexto.encarregado(), 4, ImagemEvidenciaTestSupport.imagem("png", true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("quantidade ainda retirada")));

        mockMvc.perform(get("/movimentacoes/encarregado/{id}", contexto.encarregado().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/movimentacoes/encarregado/{id}", outroEncarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertEquals(7, materialRepository.findById(contexto.material().getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void estornoPreservaEncarregadoEComprovanteHistoricoContinuaUsandoFuncionario()
            throws Exception {
        Contexto contexto = criarContexto(10);
        Usuario admin = TestUsuarioFactory.criarUsuario(
                usuarioService,
                "admin-devolucao-" + contexto.sufixo(),
                SENHA,
                Role.ADMIN,
                true
        );
        confirmarRetirada(contexto, 5);
        long devolucaoId = jsonResponse(registrarDevolucao(
                contexto,
                contexto.encarregado(),
                2,
                ImagemEvidenciaTestSupport.imagem("png", true)
        ).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        MvcResult estornoResult = mockMvc.perform(post("/movimentacoes/{id}/estorno", devolucaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(admin.getUsername())))
                        .header("Idempotency-Key", "estorno-devolucao-" + contexto.sufixo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("justificativa", "CorreÃ§Ã£o da devoluÃ§Ã£o"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ESTORNO_DEVOLUCAO"))
                .andExpect(jsonPath("$.encarregadoId").value(contexto.encarregado().getId()))
                .andReturn();
        Movimentacao estorno = movimentacaoRepository.findById(jsonResponse(estornoResult).get("id").asLong()).orElseThrow();
        assertNull(estorno.getFuncionario());
        assertEquals(contexto.encarregado().getId(), estorno.getEncarregadoAssinante().getId());
        assertEquals(5, materialRepository.findById(contexto.material().getId()).orElseThrow().getQuantidadeEstoque());

        Funcionario funcionario = funcionarioRepository.save(new Funcionario(
                "FuncionÃ¡rio histÃ³rico " + contexto.sufixo(),
                TestUsuarioFactory.proximoCpf(),
                "TÃ©cnico"
        ));
        Movimentacao historica = new Movimentacao();
        historica.setFuncionario(funcionario);
        historica.setContrato(contexto.contrato());
        historica.setMaterial(contexto.material());
        historica.setQuantidade(1);
        historica.setTipo(TipoMovimentacao.RETIRADA);
        historica.setDataMovimentacao(LocalDateTime.now());
        historica.setDataFinalizacao(LocalDateTime.now());
        historica.setRegistradoPor(contexto.operador());
        historica = movimentacaoRepository.saveAndFlush(historica);
        comprovanteService.registrar(historica);

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", historica.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionario.id").value(funcionario.getId()))
                .andExpect(jsonPath("$.funcionario.nome").value(funcionario.getNome()))
                .andExpect(jsonPath("$.encarregadoAssinante").doesNotExist());
    }

    private Contexto criarContexto(int estoque) throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(
                usuarioService, "operador-dev-" + sufixo, SENHA, Role.OPERADOR, true
        );
        Usuario encarregado = TestUsuarioFactory.criarUsuario(
                usuarioService, "encarregado-dev-" + sufixo, SENHA, Role.ENCARREGADO, true
        );
        Contrato contrato = contratoRepository.save(new Contrato(
                "Contrato devoluÃ§Ã£o " + sufixo, "Teste", true
        ));
        Material material = materialRepository.save(new Material(
                "Material devoluÃ§Ã£o " + sufixo, "Teste", estoque
        ));
        return new Contexto(
                sufixo,
                operador,
                encarregado,
                contrato,
                material,
                token(operador.getUsername()),
                token(encarregado.getUsername())
        );
    }

    private void confirmarRetirada(Contexto contexto, int quantidade) throws Exception {
        MvcResult criada = mockMvc.perform(post("/solicitacoes-retirada")
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken()))
                        .header("Idempotency-Key", "retirada-" + contexto.sufixo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "encarregadoAssinanteId", contexto.encarregado().getId(),
                                "contratoId", contexto.contrato().getId(),
                                "itens", List.of(Map.of(
                                        "materialId", contexto.material().getId(),
                                        "quantidade", quantidade
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long solicitacaoId = jsonResponse(criada).get("id").asLong();
        mockMvc.perform(multipart("/solicitacoes-retirada/{id}/confirmar", solicitacaoId)
                        .file(ImagemEvidenciaTestSupport.assinatura())
                        .header(HttpHeaders.AUTHORIZATION, bearer(contexto.encarregadoToken())))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions registrarDevolucao(
            Contexto contexto,
            Usuario encarregado,
            int quantidade,
            byte[] assinatura
    ) throws Exception {
        return registrarDevolucao(contexto, encarregado, quantidade, assinatura, true);
    }

    private org.springframework.test.web.servlet.ResultActions registrarDevolucao(
            Contexto contexto,
            Usuario encarregado,
            int quantidade,
            byte[] assinatura,
            boolean comFoto
    ) throws Exception {
        Map<String, Object> request = Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contexto.contrato().getId(),
                "materialId", contexto.material().getId(),
                "quantidade", quantidade,
                "tipo", "DEVOLUCAO"
        );
        var multipartRequest = multipart("/movimentacoes")
                .file(ImagemEvidenciaTestSupport.dados(json(request)))
                .file(new MockMultipartFile(
                        "assinatura",
                        "assinatura.png",
                        MediaType.IMAGE_PNG_VALUE,
                        assinatura
                ));
        if (comFoto) multipartRequest.file(ImagemEvidenciaTestSupport.foto());
        return mockMvc.perform(multipartRequest
                .header(HttpHeaders.AUTHORIZATION, bearer(contexto.operadorToken()))
                .header("Idempotency-Key", "devolucao-" + UUID.randomUUID()));
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
            String sufixo,
            Usuario operador,
            Usuario encarregado,
            Contrato contrato,
            Material material,
            String operadorToken,
            String encarregadoToken
    ) {
    }
}
