package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import com.Lucca.Projeto1.repository.RequisicaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.Lucca.Projeto1.storage.EvidenciaStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovimentacaoRollbackIntegrationTests {
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

    @MockitoBean private EvidenciaStorage storage;

    private Long encarregadoId;
    private Contrato contrato;
    private Material material;
    private String operadorToken;

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

        TestUsuarioFactory.criarUsuario(usuarios, "Operador", "52998224725", "31999999999", "operador", SENHA, Role.OPERADOR, true);
        TestUsuarioFactory.criarUsuario(usuarios, "Encarregado", "93541134780", "31999999998", "encarregado", SENHA, Role.ENCARREGADO, true);
        encarregadoId = usuarioRepository.findByUsernameIgnoreCase("encarregado").orElseThrow().getId();
        contrato = contratos.save(new Contrato("Contrato rollback", "Obra", true));
        material = materiais.save(new Material("Material rollback", "Teste transacional", 10));
        operadorToken = token();
    }

    @Test
    void falhaAoArmazenarEvidenciaReverteEstoqueMovimentacaoEComprovante() throws Exception {
        doThrow(new IllegalStateException("falha simulada no armazenamento"))
                .when(storage).armazenar(anyString(), any(byte[].class));

        String payload = mapper.writeValueAsString(Map.of(
                "encarregadoId", encarregadoId,
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 4,
                "tipo", "RETIRADA"
        ));
        mvc.perform(movimentacaoAssinada(payload)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operadorToken))
                .andExpect(status().isInternalServerError());

        assertEquals(10, materiais.findById(material.getId()).orElseThrow().getQuantidadeEstoque());
        assertEquals(0, movimentacoes.count());
        assertEquals(0, evidencias.count());
        assertEquals(0, comprovantes.count());
        verify(storage, atLeastOnce()).remover(anyString());
    }

    private String token() throws Exception {
        String body = mapper.writeValueAsString(Map.of("username", "operador", "password", SENHA));
        String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("token").asText();
    }
}
