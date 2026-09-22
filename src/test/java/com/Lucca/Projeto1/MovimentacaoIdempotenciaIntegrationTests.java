package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.*;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.Map;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovimentacaoIdempotenciaIntegrationTests {

    private static final String SENHA_OPERADOR =
            "senhaOperador123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private UsuarioRepository encarregadoRepository;

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

    private String operadorToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        evidenciaRepository.deleteAll();
        comprovanteRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        encarregadoRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(usuarioService,
                "operador",
                SENHA_OPERADOR,
                Role.OPERADOR,
                true
        );

        operadorToken =
                token("operador", SENHA_OPERADOR);
    }

    @Test
    void repetirMesmaOperacaoComMesmaIdempotencyKeyNaoDuplicaRetirada()
            throws Exception {

        // ARRANGE
        Material material = materialRepository.save(
                new Material(
                        "Capacete",
                        "Descrição",
                        10
                )
        );

        Usuario encarregado = encarregadoRepository.save(
                TestUsuarioFactory.encarregado(
                        "João Silva",
                        "12345678909",
                        "Pedreiro"
                )
        );

        Contrato contrato = contratoRepository.save(
                new Contrato(
                        "Contrato A",
                        "Descrição",
                        true
                )
        );

        String idempotencyKey =
                "teste-operacao-123";

        String json = json(Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 2,
                "tipo", "RETIRADA"
        ));

        // ACT - primeira tentativa
        mockMvc.perform(
                        movimentacaoAssinada(json)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                )
                .andExpect(status().isCreated());
        Movimentacao primeiraMovimentacao =
                movimentacaoRepository
                        .findAll()
                        .get(0);

        assertEquals(
                1,
                movimentacaoRepository.count()
        );

        assertEquals(
                idempotencyKey,
                primeiraMovimentacao.getIdempotencyKey()
        );

        assertNotNull(
                primeiraMovimentacao.getRequestFingerprint()
        );

        assertTrue(
                movimentacaoRepository
                        .findByRegistradoPorIdAndIdempotencyKey(
                                primeiraMovimentacao
                                        .getRegistradoPor()
                                        .getId(),
                                idempotencyKey
                        )
                        .isPresent()
        );



        // ACT - usuário repete porque acha que a primeira falhou
        mockMvc.perform(
                        movimentacaoAssinada(json)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                )
                .andExpect(status().is2xxSuccessful());

        // ASSERT
        Material atualizado =
                materialRepository
                        .findById(material.getId())
                        .orElseThrow();

        assertEquals(
                8,
                atualizado.getQuantidadeEstoque()
        );

        assertEquals(
                1,
                movimentacaoRepository.count()
        );

        assertEquals(
                1,
                comprovanteRepository.count()
        );
    }

    private String token(
            String username,
            String password
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                json(Map.of(
                                                        "username",
                                                        username,
                                                        "password",
                                                        password
                                                ))
                                        )
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
                result.getResponse()
                        .getContentAsString()
        );
    }

    private String json(Object value)
            throws Exception {

        return objectMapper
                .writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
    @Test
    void chavesDiferentesPermitemDuasRetiradasIguais()
            throws Exception {

        // ARRANGE
        Material material = materialRepository.save(
                new Material(
                        "Luva",
                        "Par de luvas",
                        10
                )
        );

        Usuario encarregado = encarregadoRepository.save(
                TestUsuarioFactory.encarregado(
                        "João Silva",
                        "12345678909",
                        "Pedreiro"
                )
        );

        Contrato contrato = contratoRepository.save(
                new Contrato(
                        "Contrato A",
                        "Descrição",
                        true
                )
        );

        String json = json(Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 2,
                "tipo", "RETIRADA"
        ));

        // ACT - primeira operação legítima
        mockMvc.perform(
                        movimentacaoAssinada(json)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                                .header(
                                        "Idempotency-Key",
                                        "operacao-001"
                                )
                )
                .andExpect(status().isCreated());

        // ACT - segunda operação legítima
        mockMvc.perform(
                        movimentacaoAssinada(json)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                                .header(
                                        "Idempotency-Key",
                                        "operacao-002"
                                )
                )
                .andExpect(status().isCreated());

        // ASSERT
        Material atualizado =
                materialRepository
                        .findById(material.getId())
                        .orElseThrow();

        assertEquals(
                6,
                atualizado.getQuantidadeEstoque()
        );

        assertEquals(
                2,
                movimentacaoRepository.count()
        );

        assertEquals(
                2,
                comprovanteRepository.count()
        );
    }

    @Test
    void mesmaIdempotencyKeyComOperacaoDiferenteERejeitada()
            throws Exception {

        // ARRANGE
        Material material = materialRepository.save(
                new Material(
                        "Luva",
                        "Par de luvas",
                        20
                )
        );

        Usuario encarregado = encarregadoRepository.save(
                TestUsuarioFactory.encarregado(
                        "João Silva",
                        "12345678909",
                        "Pedreiro"
                )
        );

        Contrato contrato = contratoRepository.save(
                new Contrato(
                        "Contrato A",
                        "Descrição",
                        true
                )
        );

        String idempotencyKey =
                "operacao-reutilizada-001";

        String primeiraOperacao = json(Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 2,
                "tipo", "RETIRADA"
        ));

        String segundaOperacao = json(Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 5,
                "tipo", "RETIRADA"
        ));

        // ACT - primeira operação
        mockMvc.perform(
                        movimentacaoAssinada(primeiraOperacao)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                )
                .andExpect(status().isCreated());

        // ACT - tenta reutilizar a mesma key para outra operação
        mockMvc.perform(
                        movimentacaoAssinada(segundaOperacao)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                )
                .andExpect(status().isConflict());

        // ASSERT
        Material atualizado =
                materialRepository
                        .findById(material.getId())
                        .orElseThrow();

        assertEquals(
                18,
                atualizado.getQuantidadeEstoque()
        );

        assertEquals(
                1,
                movimentacaoRepository.count()
        );

        assertEquals(
                1,
                comprovanteRepository.count()
        );
    }

    @Test
    void duasRequisicoesSimultaneasComMesmaIdempotencyKeyExecutamUmaUnicaRetirada()
            throws Exception {

        // ARRANGE
        Material material = materialRepository.save(
                new Material(
                        "Luva",
                        "Par de luvas",
                        10
                )
        );

        Usuario encarregado = encarregadoRepository.save(
                TestUsuarioFactory.encarregado(
                        "João Silva",
                        "12345678909",
                        "Pedreiro"
                )
        );

        Contrato contrato = contratoRepository.save(
                new Contrato(
                        "Contrato A",
                        "Descrição",
                        true
                )
        );

        String idempotencyKey =
                "operacao-concorrente-001";

        String json = json(Map.of(
                "encarregadoId", encarregado.getId(),
                "contratoId", contrato.getId(),
                "materialId", material.getId(),
                "quantidade", 2,
                "tipo", "RETIRADA"
        ));

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch prontas =
                new CountDownLatch(2);

        CountDownLatch iniciar =
                new CountDownLatch(1);

        Callable<Integer> requisicao = () -> {

            /*
             * As duas threads avisam:
             * "estou pronta".
             */
            prontas.countDown();

            /*
             * As duas ficam esperando aqui.
             */
            iniciar.await(
                    5,
                    TimeUnit.SECONDS
            );

            /*
             * Quando forem liberadas, ambas tentam
             * registrar exatamente a mesma operação.
             */
            return mockMvc.perform(
                            movimentacaoAssinada(json)
                                    .header(
                                            HttpHeaders.AUTHORIZATION,
                                            bearer(operadorToken)
                                    )
                                    .header(
                                            "Idempotency-Key",
                                            idempotencyKey
                                    )
                    )
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        Future<Integer> primeira = null;
        Future<Integer> segunda = null;

        try {

            primeira =
                    executor.submit(requisicao);

            segunda =
                    executor.submit(requisicao);

            /*
             * Esperamos confirmar que as duas
             * threads chegaram ao ponto de largada.
             */
            assertTrue(
                    prontas.await(
                            5,
                            TimeUnit.SECONDS
                    )
            );

            // ACT
            iniciar.countDown();

            int statusPrimeira =
                    primeira.get(
                            10,
                            TimeUnit.SECONDS
                    );

            int statusSegunda =
                    segunda.get(
                            10,
                            TimeUnit.SECONDS
                    );

            /*
             * As duas chamadas devem ser consideradas
             * bem-sucedidas.
             *
             * Uma realmente executa a retirada.
             * A outra reconhece o retry.
             */
            assertTrue(
                    statusPrimeira >= 200
                            && statusPrimeira < 300
            );

            assertTrue(
                    statusSegunda >= 200
                            && statusSegunda < 300
            );

        } finally {
            executor.shutdownNow();
        }


        // ASSERT
        Material atualizado =
                materialRepository
                        .findById(material.getId())
                        .orElseThrow();

        assertEquals(
                8,
                atualizado.getQuantidadeEstoque()
        );

        assertEquals(
                1,
                movimentacaoRepository.count()
        );

        assertEquals(
                1,
                comprovanteRepository.count()
        );
    }
}
