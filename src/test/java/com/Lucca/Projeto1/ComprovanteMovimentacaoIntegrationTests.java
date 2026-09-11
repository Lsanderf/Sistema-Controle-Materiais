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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComprovanteMovimentacaoIntegrationTests {

    private static final String SENHA_ADMIN = "senhaAdmin123";
    private static final String SENHA_OPERADOR = "senhaOperador123";
    private static final String SENHA_CONSULTA = "senhaConsulta123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComprovanteMovimentacaoRepository comprovanteRepository;

    @Autowired
    private EvidenciaMovimentacaoRepository evidenciaRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private NotaFiscalEntradaRepository notaFiscalRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    private String adminToken;
    private String operadorToken;
    private String consultaToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        evidenciaRepository.deleteAll();
        comprovanteRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        notaFiscalRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.criarUsuario("admin", SENHA_ADMIN, Role.ADMIN, true);
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

    @Test
    void consultarComprovanteExistenteRetornaSnapshotCompleto() throws Exception {
        Contexto contexto = criarContexto(10);
        Long movimentacaoId = id(registrarMovimentacao(
                contexto,
                TipoMovimentacao.RETIRADA,
                3,
                "Entregue no almoxarifado móvel"
        ));

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movimentacaoId))
                .andExpect(jsonPath("$.tipo").value("RETIRADA"))
                .andExpect(jsonPath("$.quantidade").value(3))
                .andExpect(jsonPath("$.dataMovimentacao").isNotEmpty())
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty())
                .andExpect(jsonPath("$.observacao").value(
                        "Entregue no almoxarifado móvel"
                ))
                .andExpect(jsonPath("$.material.id")
                        .value(contexto.material().getId()))
                .andExpect(jsonPath("$.material.nome").value("Capacete"))
                .andExpect(jsonPath("$.funcionario.id")
                        .value(contexto.funcionario().getId()))
                .andExpect(jsonPath("$.funcionario.nome").value("João Silva"))
                .andExpect(jsonPath("$.funcionario.cargo").value("Pedreiro"))
                .andExpect(jsonPath("$.contrato.id")
                        .value(contexto.contrato().getId()))
                .andExpect(jsonPath("$.registradoPor.username").value("operador"))
                .andExpect(jsonPath("$.notaFiscal").isEmpty())
                .andExpect(jsonPath("$.evidencias.length()").value(0))
                .andExpect(jsonPath("$.versao").value(1));
    }

    @Test
    void consultarComprovanteDeMovimentacaoInexistenteRetornaNotFound()
            throws Exception {
        mockMvc.perform(get("/movimentacoes/{id}/comprovante", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(
                        "Movimentação com ID 999999 não encontrada"
                ));
    }

    @Test
    void comprovanteDeEntradaMantemVinculoEDadosDaNotaFiscal()
            throws Exception {
        Material material = criarMaterial("Luva", 0);
        String chave = String.format("%044d", 91);

        MvcResult criacao = mockMvc.perform(post("/notas-fiscais")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "numero", "NF-908",
                                "serie", "3",
                                "chaveAcesso", chave,
                                "fornecedor", "Fornecedor Seguro Ltda",
                                "cnpjFornecedor", "11222333000181",
                                "dataEmissao", LocalDate.now().toString(),
                                "itens", List.of(Map.of(
                                        "materialId", material.getId(),
                                        "quantidade", 5,
                                        "valorUnitario", new BigDecimal("12.50")
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long notaId = id(criacao);

        MvcResult confirmacao = mockMvc.perform(
                        post("/notas-fiscais/{id}/confirmar", notaId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                )
                .andExpect(status().isOk())
                .andReturn();
        Long movimentacaoId = jsonResponse(confirmacao)
                .get("movimentacoes").get(0).get("id").asLong();

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.funcionario").isEmpty())
                .andExpect(jsonPath("$.contrato").isEmpty())
                .andExpect(jsonPath("$.notaFiscal.id").value(notaId))
                .andExpect(jsonPath("$.notaFiscal.numero").value("NF-908"))
                .andExpect(jsonPath("$.notaFiscal.serie").value("3"))
                .andExpect(jsonPath("$.notaFiscal.chaveAcesso").value(chave))
                .andExpect(jsonPath("$.notaFiscal.fornecedor")
                        .value("Fornecedor Seguro Ltda"))
                .andExpect(jsonPath("$.notaFiscal.cnpjFornecedor")
                        .value("11222333000181"));
    }

    @Test
    void comprovanteDeDevolucaoRetornaFuncionarioContratoEOperador()
            throws Exception {
        Contexto contexto = criarContexto(10);
        registrarMovimentacao(
                contexto,
                TipoMovimentacao.RETIRADA,
                6,
                null
        );
        Long devolucaoId = id(registrarMovimentacao(
                contexto,
                TipoMovimentacao.DEVOLUCAO,
                4,
                "Material sem avarias"
        ));

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", devolucaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DEVOLUCAO"))
                .andExpect(jsonPath("$.quantidade").value(4))
                .andExpect(jsonPath("$.funcionario.nome").value("João Silva"))
                .andExpect(jsonPath("$.contrato.nome").value("Contrato A"))
                .andExpect(jsonPath("$.material.nome").value("Capacete"))
                .andExpect(jsonPath("$.registradoPor.username").value("operador"))
                .andExpect(jsonPath("$.observacao").value("Material sem avarias"))
                .andExpect(jsonPath("$.notaFiscal").isEmpty());
    }

    @Test
    void snapshotNaoMudaQuandoCadastrosRelacionadosSaoEditados()
            throws Exception {
        Contexto contexto = criarContexto(10);
        Long movimentacaoId = id(registrarMovimentacao(
                contexto,
                TipoMovimentacao.RETIRADA,
                2,
                null
        ));

        contexto.material().setNome("Capacete renomeado");
        materialRepository.saveAndFlush(contexto.material());
        contexto.funcionario().setNome("Nome atualizado");
        funcionarioRepository.saveAndFlush(contexto.funcionario());
        contexto.contrato().setNome("Contrato atualizado");
        contratoRepository.saveAndFlush(contexto.contrato());

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.material.nome").value("Capacete"))
                .andExpect(jsonPath("$.funcionario.nome").value("João Silva"))
                .andExpect(jsonPath("$.contrato.nome").value("Contrato A"));
    }

    @Test
    void endpointDeMovimentacaoNaoPermiteEdicaoAposConclusao()
            throws Exception {
        Contexto contexto = criarContexto(10);
        Long movimentacaoId = id(registrarMovimentacao(
                contexto,
                TipoMovimentacao.RETIRADA,
                2,
                null
        ));

        mockMvc.perform(put("/movimentacoes/{id}", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantidade", 9))))
                .andExpect(status().isMethodNotAllowed());

        assertEquals(
                2,
                movimentacaoRepository.findById(movimentacaoId)
                        .orElseThrow().getQuantidade()
        );
    }

    @Test
    void assinaturaEArmazenadaComoEvidenciaImutavelEProtegidaPorPerfil()
            throws Exception {
        Contexto contexto = criarContexto(10);
        Long movimentacaoId = id(registrarMovimentacao(
                contexto,
                TipoMovimentacao.RETIRADA,
                2,
                null
        ));
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47,
                0x0d, 0x0a, 0x1a, 0x0a,
                0x01, 0x02, 0x03
        };
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                MediaType.IMAGE_PNG_VALUE,
                png
        );

        MvcResult upload = mockMvc.perform(
                        multipart("/movimentacoes/{id}/assinatura", movimentacaoId)
                                .file(arquivo)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ASSINATURA"))
                .andExpect(jsonPath("$.funcionario.id")
                        .value(contexto.funcionario().getId()))
                .andExpect(jsonPath("$.registradaPor.username").value("operador"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.sha256").value(
                        "7f47b756761a46e6d4a4d96f0d8a4448f8449235009d1f3ad1493f5c773c19e8"
                ))
                .andReturn();
        JsonNode evidencia = jsonResponse(upload);
        Long evidenciaId = evidencia.get("id").asLong();

        mockMvc.perform(get(
                                "/movimentacoes/{movimentacaoId}/evidencias/{evidenciaId}/arquivo",
                                movimentacaoId,
                                evidenciaId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(png))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

        mockMvc.perform(get("/movimentacoes/{id}/comprovante", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(consultaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidencias.length()").value(1))
                .andExpect(jsonPath("$.evidencias[0].id").value(evidenciaId))
                .andExpect(jsonPath("$.evidencias[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.evidencias[0].urlArquivo").value(
                        "/movimentacoes/" + movimentacaoId
                                + "/evidencias/" + evidenciaId + "/arquivo"
                ));

        mockMvc.perform(
                        multipart("/movimentacoes/{id}/assinatura", movimentacaoId)
                                .file(arquivo)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                )
                .andExpect(status().isConflict());

        mockMvc.perform(
                        multipart("/movimentacoes/{id}/assinatura", movimentacaoId)
                                .file(arquivo)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(consultaToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    private MvcResult registrarMovimentacao(
            Contexto contexto,
            TipoMovimentacao tipo,
            int quantidade,
            String observacao
    ) throws Exception {
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("funcionarioId", contexto.funcionario().getId());
        request.put("contratoId", contexto.contrato().getId());
        request.put("materialId", contexto.material().getId());
        request.put("quantidade", quantidade);
        request.put("tipo", tipo.name());
        if (observacao != null) {
            request.put("observacao", observacao);
        }

        return mockMvc.perform(post("/movimentacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private Contexto criarContexto(int estoque) {
        return new Contexto(
                funcionarioRepository.save(
                        new Funcionario("João Silva", "12345678909", "Pedreiro")
                ),
                contratoRepository.save(
                        new Contrato("Contrato A", "Obra principal", true)
                ),
                criarMaterial("Capacete", estoque)
        );
    }

    private Material criarMaterial(String nome, int estoque) {
        return materialRepository.save(
                new Material(nome, "Descrição do material", estoque)
        );
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
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Contexto(
            Funcionario funcionario,
            Contrato contrato,
            Material material
    ) {
    }
}
