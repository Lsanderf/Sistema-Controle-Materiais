package com.Lucca.Projeto1;

import com.Lucca.Projeto1.repository.MaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

/**
 * Executar com -Dsolicitacao.postgres.tests=true, TEST_DB_USER/TEST_DB_PASSWORD e o perfil postgres-audit.
 * Usa PostgreSQL real e Flyway: H2/create-drop não aplica o índice parcial da migration.
 */
@ActiveProfiles("postgres-audit")
@EnabledIfSystemProperty(named = "solicitacao.postgres.tests", matches = "true")
class SolicitacaoRetiradaPendenciaPostgresIntegrationTests extends SolicitacaoRetiradaPendenciaIntegrationTests {

    @MockitoSpyBean private MaterialRepository materiaisComBloqueio;

    @Test
    void indiceParcialImpedeSegundaPendenciaMesmoSemPassarPeloService() throws Exception {
        Cenario cenario = cenario();
        primeira(cenario);

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("insert into tb_solicitacoes_retirada "
                                + "(operador_responsavel_id, encarregado_assinante_id, status, criada_em) "
                                + "values (?, ?, 'AGUARDANDO_ASSINATURA', current_timestamp)",
                        cenario.outroOperador().getId(), cenario.encarregado().getId()));

        PSQLException causa = (PSQLException) exception.getMostSpecificCause();
        assertEquals("23505", causa.getSQLState());
        assertEquals("uk_solicitacoes_retirada_encarregado_pendente", causa.getServerErrorMessage().getConstraint());
        String indice = jdbc.queryForObject("select indexdef from pg_indexes "
                + "where indexname = 'uk_solicitacoes_retirada_encarregado_pendente'", String.class);
        assertTrue(indice.contains("UNIQUE"));
        assertTrue(indice.contains("encarregado_assinante_id"));
        assertTrue(indice.contains("WHERE"));
        assertTrue(indice.contains("AGUARDANDO_ASSINATURA"));
        assertSemEfeitosDeConfirmacao(cenario, 1);
    }

    @Test
    void requisicoesConcorrentesAposValidacaoAntecipadaResultamEm201E409SemDadosParciais() throws Exception {
        Cenario cenario = cenario();
        CyclicBarrier aposVerificacaoDePendencia = new CyclicBarrier(2);
        var consultaReal = mockingDetails(materiaisComBloqueio).getMockCreationSettings().getDefaultAnswer();
        // Ambas as requisições já passaram pela consulta de pendência ao alcançar este ponto.
        // Materiais distintos evitam que seus locks serializem o teste antes do INSERT.
        doAnswer(invocation -> {
            aposVerificacaoDePendencia.await(15, TimeUnit.SECONDS);
            return consultaReal.answer(invocation);
        }).when(materiaisComBloqueio).findAllByIdComBloqueio(anyCollection());

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> criar(cenario, cenario.tokenOperador(),
                    cenario.contrato(), cenario.material(), "concorrente-a").andReturn());
            var segunda = executor.submit(() -> criar(cenario, cenario.tokenOutroOperador(),
                    cenario.outroContrato(), cenario.outroMaterial(), "concorrente-b").andReturn());
            List<MvcResult> resultados = List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));

            assertEquals(List.of(201, 409), resultados.stream().map(r -> r.getResponse().getStatus()).sorted().toList(),
                    () -> resultados.stream().map(r -> String.valueOf(r.getResolvedException())).toList().toString());
            MvcResult conflito = resultados.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
            assertEquals(CONFLITO_PENDENCIA, objectMapper.readTree(conflito.getResponse().getContentAsString()).get("erro").asText());
        }
        assertSemEfeitosDeConfirmacao(cenario, 1);
    }
}
