package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.EvidenciaMovimentacao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.EvidenciaMovimentacaoService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Map;

import static com.Lucca.Projeto1.ImagemEvidenciaTestSupport.movimentacaoAssinada;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.evidencias.storage=local",
        "app.evidencias.diretorio=./target/test-evidencias-integridade"
})
@AutoConfigureMockMvc
class EvidenciaIntegridadeIntegrationTests {

    private static final String SENHA_OPERADOR =
            "senhaOperador123";

    private static final Path DIRETORIO_EVIDENCIAS =
            Path.of("./target/test-evidencias-integridade")
                    .toAbsolutePath()
                    .normalize();

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

    @Autowired
    private EvidenciaMovimentacaoService evidenciaService;

    private String operadorToken;

    @BeforeEach
    void preparar() throws Exception {

        apagarDiretorioDeTeste();

        evidenciaRepository.deleteAll();
        comprovanteRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(
                usuarioService,
                "operador",
                SENHA_OPERADOR,
                Role.OPERADOR,
                true
        );

        operadorToken =
                token("operador", SENHA_OPERADOR);
    }

    @AfterEach
    void limparArquivos() throws Exception {
        apagarDiretorioDeTeste();
    }

    @Test
    void arquivoAdulteradoEDetectadoPeloSha256()
            throws Exception {

        // ARRANGE ------------------------------------------

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
         * Cria uma retirada real, inclusive salvando
         * a assinatura fisicamente.
         */
        mockMvc.perform(
                        movimentacaoAssinada(
                                json(Map.of(
                                        "funcionarioId",
                                        funcionario.getId(),

                                        "contratoId",
                                        contrato.getId(),

                                        "materialId",
                                        material.getId(),

                                        "quantidade",
                                        2,

                                        "tipo",
                                        "RETIRADA"
                                ))
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(operadorToken)
                                )
                )
                .andExpect(status().isCreated());


        Movimentacao movimentacao =
                movimentacaoRepository
                        .findAll()
                        .get(0);

        EvidenciaMovimentacao evidencia =
                evidenciaRepository
                        .findAll()
                        .get(0);


        /*
         * Antes de adulterar, a integridade deve estar OK.
         */
        assertDoesNotThrow(
                () -> evidenciaService.buscarArquivo(
                        movimentacao.getId(),
                        evidencia.getId()
                )
        );


        /*
         * Descobrimos o caminho físico a partir
         * da storageKey registrada no banco.
         */
        Path arquivo =
                DIRETORIO_EVIDENCIAS
                        .resolve(evidencia.getStorageKey())
                        .normalize();


        // ACT ----------------------------------------------

        /*
         * Lemos os bytes originais.
         */
        byte[] conteudo =
                Files.readAllBytes(arquivo);

        /*
         * Alteramos somente UM byte.
         *
         * Muito importante:
         * não mudamos o tamanho do arquivo.
         *
         * Assim, o teste prova especificamente
         * que o conteúdo/hash mudou.
         */
        int posicao =
                conteudo.length / 2;

        conteudo[posicao] =
                (byte) (conteudo[posicao] ^ 1);

        Files.write(
                arquivo,
                conteudo,
                StandardOpenOption.TRUNCATE_EXISTING
        );


        // ASSERT -------------------------------------------

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> evidenciaService.buscarArquivo(
                                movimentacao.getId(),
                                evidencia.getId()
                        )
                );

        assertEquals(
                "O arquivo da evidência falhou na verificação de integridade",
                exception.getMessage()
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


    private JsonNode jsonResponse(
            MvcResult result
    ) throws Exception {

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


    private void apagarDiretorioDeTeste()
            throws Exception {

        if (!Files.exists(DIRETORIO_EVIDENCIAS)) {
            return;
        }

        try (var caminhos =
                     Files.walk(DIRETORIO_EVIDENCIAS)) {

            for (Path caminho :
                    caminhos
                            .sorted(Comparator.reverseOrder())
                            .toList()) {

                Files.deleteIfExists(caminho);
            }
        }
    }
}
