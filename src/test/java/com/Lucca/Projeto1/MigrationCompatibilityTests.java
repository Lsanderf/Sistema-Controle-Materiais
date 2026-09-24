package com.Lucca.Projeto1;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationCompatibilityTests {

    @Test
    void migrationV12CriaRequisicoesEItensComRelacionamentos() throws Exception {
        String databaseName = "migration_"
                + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()
        ) {
            statement.execute("CREATE TABLE tb_usuarios (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE tb_contratos (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE tb_materiais (id BIGINT PRIMARY KEY)");
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("11")
                .target("12")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);
        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME IN ('TB_REQUISICOES', 'TB_REQUISICAO_ITENS')
                        """)
        ) {
            assertTrue(rows.next());
            assertEquals(2, rows.getInt(1));
        }
    }

    @Test
    void migrationV13PreservaDescricaoDosItensLegadosERemoveDependenciaDeMaterial() throws Exception {
        String databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()
        ) {
            statement.execute("CREATE TABLE tb_materiais (id BIGINT PRIMARY KEY, nome VARCHAR(150) NOT NULL)");
            statement.execute("CREATE TABLE tb_requisicoes (id BIGINT PRIMARY KEY)");
            statement.execute("""
                    CREATE TABLE tb_requisicao_itens (
                        id BIGINT PRIMARY KEY,
                        requisicao_id BIGINT NOT NULL,
                        material_id BIGINT NOT NULL,
                        quantidade INTEGER NOT NULL,
                        CONSTRAINT uk_requisicao_itens_material UNIQUE (requisicao_id, material_id),
                        CONSTRAINT fk_requisicao_itens_requisicao FOREIGN KEY (requisicao_id) REFERENCES tb_requisicoes(id),
                        CONSTRAINT fk_requisicao_itens_material FOREIGN KEY (material_id) REFERENCES tb_materiais(id)
                    )
                    """);
            statement.execute("INSERT INTO tb_requisicoes (id) VALUES (1)");
            statement.execute("INSERT INTO tb_materiais (id, nome) VALUES (1, 'Capacete')");
            statement.execute("INSERT INTO tb_requisicao_itens (id, requisicao_id, material_id, quantidade) VALUES (1, 1, 1, 5)");
        }

        MigrateResult result = Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/migration").baselineOnMigrate(true)
                .baselineVersion("12").target("13").load().migrate();

        assertEquals(1, result.migrationsExecuted);
        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                Statement columnStatement = connection.createStatement();
                ResultSet item = statement.executeQuery("SELECT descricao, quantidade FROM tb_requisicao_itens WHERE id = 1");
                ResultSet column = columnStatement.executeQuery("""
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME = 'TB_REQUISICAO_ITENS' AND COLUMN_NAME = 'MATERIAL_ID'
                        """)
        ) {
            assertTrue(item.next());
            assertEquals("Capacete", item.getString("descricao"));
            assertEquals(5, item.getInt("quantidade"));
            assertTrue(column.next());
            assertEquals(0, column.getInt(1));
        }
    }

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
                .target("6")
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

    @Test
    void migrationV7PreservaMovimentacoesAntigasEGeraComprovante()
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
                    CREATE TABLE tb_usuarios (
                        id BIGINT PRIMARY KEY,
                        username VARCHAR(100) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tb_funcionarios (
                        id BIGINT PRIMARY KEY,
                        nome VARCHAR(150) NOT NULL,
                        cargo VARCHAR(100) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tb_contratos (
                        id BIGINT PRIMARY KEY,
                        nome VARCHAR(150) NOT NULL,
                        descricao VARCHAR(500)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tb_materiais (
                        id BIGINT PRIMARY KEY,
                        nome VARCHAR(150) NOT NULL,
                        descricao VARCHAR(500)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tb_notas_fiscais (
                        id BIGINT PRIMARY KEY,
                        numero VARCHAR(50) NOT NULL,
                        serie VARCHAR(20) NOT NULL,
                        chave_acesso VARCHAR(44) NOT NULL,
                        fornecedor VARCHAR(200) NOT NULL,
                        cnpj_fornecedor VARCHAR(14) NOT NULL,
                        data_emissao DATE NOT NULL,
                        data_entrada TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tb_movimentacoes (
                        id BIGINT PRIMARY KEY,
                        funcionario_id BIGINT,
                        contrato_id BIGINT,
                        material_id BIGINT NOT NULL,
                        quantidade INTEGER NOT NULL,
                        tipo VARCHAR(20) NOT NULL,
                        data_movimentacao TIMESTAMP NOT NULL,
                        usuario_id BIGINT,
                        nota_fiscal_id BIGINT
                    )
                    """);

            statement.execute(
                    "INSERT INTO tb_usuarios VALUES (1, 'operador-legado')"
            );
            statement.execute(
                    "INSERT INTO tb_funcionarios VALUES (1, 'João', 'Pedreiro')"
            );
            statement.execute(
                    "INSERT INTO tb_contratos VALUES (1, 'Obra A', 'Contrato legado')"
            );
            statement.execute(
                    "INSERT INTO tb_materiais VALUES (1, 'Capacete', 'EPI')"
            );
            statement.execute("""
                    INSERT INTO tb_movimentacoes (
                        id, funcionario_id, contrato_id, material_id,
                        quantidade, tipo, data_movimentacao, usuario_id
                    ) VALUES (
                        10, 1, 1, 1, 2, 'RETIRADA',
                        TIMESTAMP '2026-01-02 10:30:00', 1
                    )
                    """);
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("6")
                .target("7")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("""
                        SELECT movimentacao_id, material_nome, funcionario_nome,
                               usuario_username, data_finalizacao, versao
                        FROM tb_comprovantes_movimentacao
                        WHERE movimentacao_id = 10
                        """)
        ) {
            assertTrue(rows.next());
            assertEquals(10L, rows.getLong("movimentacao_id"));
            assertEquals("Capacete", rows.getString("material_nome"));
            assertEquals("João", rows.getString("funcionario_nome"));
            assertEquals("operador-legado", rows.getString("usuario_username"));
            assertNotNull(rows.getTimestamp("data_finalizacao"));
            assertEquals(1, rows.getInt("versao"));
        }
    }

    @Test
    void migrationV11AdicionaDadosPessoaisERestringeRolesFinais()
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
                    CREATE TABLE tb_usuarios (
                        id BIGINT PRIMARY KEY,
                        username VARCHAR(100) NOT NULL,
                        senha VARCHAR(255) NOT NULL,
                        role VARCHAR(20) NOT NULL,
                        ativo BOOLEAN NOT NULL,
                        data_inativacao TIMESTAMP,
                        CONSTRAINT ck_usuarios_role
                            CHECK (role IN ('ADMIN', 'OPERADOR', 'CONSULTA'))
                    )
                    """);
            statement.execute("""
                    INSERT INTO tb_usuarios
                        (id, username, senha, role, ativo)
                    VALUES
                        (1, 'admin', 'hash-1', 'ADMIN', TRUE),
                        (2, 'leitura-legada', 'hash-2', 'CONSULTA', TRUE)
                    """);
        }

        MigrateResult result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("10")
                .target("11")
                .load()
                .migrate();

        assertEquals(1, result.migrationsExecuted);

        try (
                Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("""
                        SELECT id, nome, cpf, celular, role
                        FROM tb_usuarios
                        ORDER BY id
                        """)
        ) {
            assertTrue(rows.next());
            assertEquals("admin", rows.getString("nome"));
            assertEquals("00000000001", rows.getString("cpf"));
            assertEquals("00000000000", rows.getString("celular"));
            assertEquals("ADMIN", rows.getString("role"));

            assertTrue(rows.next());
            assertEquals("leitura-legada", rows.getString("nome"));
            assertEquals("00000000002", rows.getString("cpf"));
            assertEquals("GERENTE", rows.getString("role"));
            assertTrue(!rows.next());

            assertThrows(SQLException.class, () -> statement.execute("""
                    INSERT INTO tb_usuarios
                        (id, nome, cpf, celular, username, senha, role, ativo)
                    VALUES
                        (3, 'Inválido', '12345678909', '11999999999',
                         'invalido', 'hash-3', 'OUTRA', TRUE)
                    """));

            assertThrows(SQLException.class, () -> statement.execute("""
                    INSERT INTO tb_usuarios
                        (id, nome, cpf, celular, username, senha, role, ativo)
                    VALUES
                        (4, 'CPF duplicado', '00000000001', '11999999999',
                         'cpf-duplicado', 'hash-4', 'ENCARREGADO', TRUE)
                    """));
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
