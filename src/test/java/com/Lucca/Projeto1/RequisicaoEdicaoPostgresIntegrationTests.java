package com.Lucca.Projeto1;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.ActiveProfiles;

/** Mesmos contratos e corridas em PostgreSQL real, com as migrations Flyway. */
@ActiveProfiles("postgres-audit")
@EnabledIfSystemProperty(named = "requisicao.postgres.tests", matches = "true")
class RequisicaoEdicaoPostgresIntegrationTests extends RequisicaoEdicaoIntegrationTests { }
