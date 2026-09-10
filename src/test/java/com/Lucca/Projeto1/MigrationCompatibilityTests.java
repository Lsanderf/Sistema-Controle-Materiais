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
                .target("4")
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
                .target("4")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            validarRegistrosAntigos(connection, "tb_usuarios");
            validarRegistrosAntigos(connection, "tb_funcionarios");
            validarRegistrosAntigos(connection, "tb_contratos");
        }
    }

    @Test
    void migrationV5PreservaMovimentacoesAntigasSemNotaFiscal()
            throws Exception {
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
                    "CREATE TABLE tb_materiais (id BIGINT PRIMARY KEY)"
            );
            statement.execute(
                    "CREATE TABLE tb_movimentacoes (id BIGINT PRIMARY KEY)"
            );
            statement.execute(
                    "INSERT INTO tb_movimentacoes (id) VALUES (1)"
            );
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("4")
                .target("5")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT nota_fiscal_id FROM tb_movimentacoes WHERE id = 1"
                )
        ) {
            assertTrue(rows.next());
            assertNull(rows.getObject("nota_fiscal_id"));
        }
    }

    @Test
    void migrationV6RemoveCaminhoArquivoSemAlterarNotasExistentes()
            throws Exception {
        String databaseName = "migration_"
                + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()
        ) {
            statement.execute("""
                    CREATE TABLE tb_notas_fiscais (
                        id BIGINT PRIMARY KEY,
                        numero VARCHAR(50) NOT NULL,
                        chave_acesso VARCHAR(44) NOT NULL,
                        caminho_arquivo VARCHAR(1000)
                    )
                    """);
            statement.execute("""
                    INSERT INTO tb_notas_fiscais
                        (id, numero, chave_acesso, caminho_arquivo)
                    VALUES
                        (1, '12345', '00000000000000000000000000000000000000000001',
                            '/tmp/nf.pdf')
                    """);
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("5")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME = 'TB_NOTAS_FISCAIS'
                            AND COLUMN_NAME = 'CAMINHO_ARQUIVO'
                        """)
        ) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
        }

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT numero, chave_acesso FROM tb_notas_fiscais WHERE id = 1"
                )
        ) {
            assertTrue(rows.next());
            assertEquals("12345", rows.getString("numero"));
            assertEquals(
                    "00000000000000000000000000000000000000000001",
                    rows.getString("chave_acesso")
            );
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
