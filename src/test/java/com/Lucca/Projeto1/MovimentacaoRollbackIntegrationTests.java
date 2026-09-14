package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.Lucca.Projeto1.storage.EvidenciaStorage;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;


import java.util.Map;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.service.ComprovanteMovimentacaoService;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovimentacaoRollbackIntegrationTests {

    private static final String SENHA_OPERADOR = "senhaOperador123";

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
    private EvidenciaMovimentacaoRepository evidenciaRepository;

    @Autowired
    private ComprovanteMovimentacaoRepository comprovanteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    /*
     * Substitui o EvidenciaStorage real por um objeto controlado
     * pelo teste.
     */
    @MockitoBean
    private EvidenciaStorage evidenciaStorage;
    @MockitoBean
    private ComprovanteMovimentacaoService comprovanteService;

    private String operadorToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        evidenciaRepository.deleteAll();
        comprovanteRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.criarUsuario(
                "operador",
                SENHA_OPERADOR,
                Role.OPERADOR,
                true
        );

        operadorToken = token("operador", SENHA_OPERADOR);
    }

    @Test
    void falhaAoArmazenarAssinaturaFazRollbackCompletoDaRetirada()
            throws Exception {

        // ARRANGE ----------------------------------------------

        Material material = materialRepository.save(
                new Material(
                        "Capacete",
                        "Descrição do material",
                        10
                )
        );

        Funcionario funcionario = funcionarioRepository.save(
                new Funcionario(
                        "João Silva",
                        "12345678909",
                        "Pedreiro"
                )
        );

        Contrato contrato = contratoRepository.save(
                new Contrato(
                        "Contrato A",
                        "Descrição do contrato",
                        true
                )
        );

        /*
         * Aqui provocamos a falha.
         *
         * Quando o sistema tentar salvar qualquer evidência,
         * o mock lançará esta exceção.
         */
        doThrow(new IllegalStateException("Falha simulada no armazenamento"))
                .when(evidenciaStorage)
                .armazenar(anyString(), any(byte[].class));

        // ACT --------------------------------------------------

        mockMvc.perform(
                        movimentacaoAssinada(
                                json(Map.of(
                                        "funcionarioId", funcionario.getId(),
                                        "contratoId", contrato.getId(),
                                        "materialId", material.getId(),
                                        "quantidade", 4,
                                        "tipo", "RETIRADA"
                                ))
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                )
                .andExpect(status().is5xxServerError());

        // ASSERT -----------------------------------------------

        /*
         * O armazenamento realmente chegou a ser chamado?
         *
         * Isso é importante porque prova que a falha simulada
         * aconteceu no ponto que queríamos testar.
         */
        verify(evidenciaStorage)
                .armazenar(anyString(), any(byte[].class));

        /*
         * Estoque inicial era 10.
         *
         * Apesar de o código ter iniciado uma retirada de 4,
         * a transação falhou.
         *
         * Portanto deve continuar 10.
         */
        Material materialDepoisDaFalha =
                materialRepository.findById(material.getId())
                        .orElseThrow();

        assertEquals(
                10,
                materialDepoisDaFalha.getQuantidadeEstoque()
        );

        /*
         * Nada da operação incompleta deve permanecer no banco.
         */
        assertEquals(0, movimentacaoRepository.count());
        assertEquals(0, comprovanteRepository.count());
        assertEquals(0, evidenciaRepository.count());
    }

    private String token(String username, String password)
            throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of(
                                        "username", username,
                                        "password", password
                                )))
                )
                .andExpect(status().isOk())
                .andReturn();

        return jsonResponse(result)
                .get("token")
                .asText();
    }

    private JsonNode jsonResponse(MvcResult result)
            throws Exception {

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
    }

    private String json(Object value)
            throws Exception {

        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }@Test
    void falhaDepoisDeSalvarEvidenciaFazRollbackERemoveArquivo()
            throws Exception {

        // ARRANGE -------------------------------------------------

        Material material = materialRepository.save(
                new Material(
                        "Capacete",
                        "Descrição do material",
                        10
                )
        );

        Funcionario funcionario = funcionarioRepository.save(
                new Funcionario(
                        "João Silva",
                        "12345678909",
                        "Pedreiro"
                )
        );

        Contrato contrato = contratoRepository.save(
                new Contrato(
                        "Contrato A",
                        "Descrição do contrato",
                        true
                )
        );

        /*
         * O EvidenciaStorage é um mock.
         *
         * Por padrão, o método armazenar() não lança erro.
         * Portanto, para este teste, consideramos que o armazenamento
         * da assinatura ocorreu normalmente.
         *
         * Depois disso, fazemos a etapa seguinte (comprovante)
         * falhar propositalmente.
         */
        doThrow(
                new IllegalStateException(
                        "Falha simulada após salvar a evidência"
                )
        )
                .when(comprovanteService)
                .registrar(any(Movimentacao.class));


        // ACT -----------------------------------------------------

        mockMvc.perform(
                        movimentacaoAssinada(
                                json(Map.of(
                                        "funcionarioId", funcionario.getId(),
                                        "contratoId", contrato.getId(),
                                        "materialId", material.getId(),
                                        "quantidade", 4,
                                        "tipo", "RETIRADA"
                                ))
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                )
                .andExpect(status().is5xxServerError());


        // ASSERT --------------------------------------------------

        /*
         * Primeiro capturamos a chave usada para armazenar
         * a assinatura.
         */
        ArgumentCaptor<String> storageKeyCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(evidenciaStorage)
                .armazenar(
                        storageKeyCaptor.capture(),
                        any(byte[].class)
                );

        String storageKey = storageKeyCaptor.getValue();


        /*
         * Isto prova que chegamos à etapa posterior à evidência.
         *
         * Se registrar() foi chamado, significa que o fluxo passou
         * pela criação da evidência e chegou ao comprovante.
         */
        verify(comprovanteService)
                .registrar(any(Movimentacao.class));


        /*
         * Como a etapa seguinte falhou e a transação fez rollback,
         * a compensação deve pedir a remoção EXATAMENTE do arquivo
         * que havia sido armazenado.
         */
        verify(evidenciaStorage, times(1))
                .remover(storageKey);


        /*
         * O estoque tinha começado em 10.
         *
         * Apesar de uma retirada de 4 ter sido iniciada,
         * o rollback deve restaurá-lo.
         */
        Material materialDepois =
                materialRepository.findById(material.getId())
                        .orElseThrow();

        assertEquals(
                10,
                materialDepois.getQuantidadeEstoque()
        );


        /*
         * Nenhuma parte da movimentação incompleta deve
         * permanecer persistida.
         */
        assertEquals(0, movimentacaoRepository.count());
        assertEquals(0, evidenciaRepository.count());
        assertEquals(0, comprovanteRepository.count());
    }

}