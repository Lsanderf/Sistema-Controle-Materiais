package com.Lucca.Projeto1;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationCompatibilityTests {

    @Test
    void migrationV3PreservaMovimentacoesAntigasSemUsuario() throws Exception {
        String databaseName = "migration_"
                + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()
        ) {
            statement.execute(
                    "CREATE TABLE tb_usuarios (id BIGINT PRIMARY KEY)"
            );
            statement.execute(
                    "CREATE TABLE tb_movimentacoes (id BIGINT PRIMARY KEY)"
            );
            statement.execute(
                    "CREATE TABLE tb_funcionarios (id BIGINT PRIMARY KEY)"
            );
            statement.execute(
                    "CREATE TABLE tb_contratos (id BIGINT PRIMARY KEY)"
            );
            statement.execute(
                    "INSERT INTO tb_movimentacoes (id) VALUES (1)"
            );
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("2")
                .load()
                .migrate();

        assertEquals(2, result.migrationsExecuted);

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT usuario_id FROM tb_movimentacoes WHERE id = 1"
                )
        ) {
            rows.next();
            assertNull(rows.getObject("usuario_id"));
        }
    }

    @Test
    void migrationV4AdicionaColunasSemInventarDatasOuAlterarStatus()
            throws Exception {
        String databaseName = "migration_"
                + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()
        ) {
            criarTabelaComRegistrosAntigos(statement, "tb_usuarios");
            criarTabelaComRegistrosAntigos(statement, "tb_funcionarios");
            criarTabelaComRegistrosAntigos(statement, "tb_contratos");
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("3")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            validarRegistrosAntigos(connection, "tb_usuarios");
            validarRegistrosAntigos(connection, "tb_funcionarios");
            validarRegistrosAntigos(connection, "tb_contratos");
        }
    }

    private void criarTabelaComRegistrosAntigos(
            Statement statement,
            String tabela
    ) throws Exception {
        statement.execute(
                "CREATE TABLE " + tabela
                        + " (id BIGINT PRIMARY KEY, ativo BOOLEAN NOT NULL)"
        );
        statement.execute(
                "INSERT INTO " + tabela
                        + " (id, ativo) VALUES (1, TRUE), (2, FALSE)"
        );
    }

    private void validarRegistrosAntigos(
            Connection connection,
            String tabela
    ) throws Exception {
        try (
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT id, ativo, data_inativacao FROM "
                                + tabela + " ORDER BY id"
                )
        ) {
            assertTrue(rows.next());
            assertEquals(1L, rows.getLong("id"));
            assertTrue(rows.getBoolean("ativo"));
            assertNull(rows.getObject("data_inativacao"));

            assertTrue(rows.next());
            assertEquals(2L, rows.getLong("id"));
            assertTrue(!rows.getBoolean("ativo"));
            assertNull(rows.getObject("data_inativacao"));

            assertTrue(!rows.next());
        }
    }
}
