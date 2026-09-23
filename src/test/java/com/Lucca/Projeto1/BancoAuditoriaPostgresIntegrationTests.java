package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.ComprovanteMovimentacao;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.EvidenciaMovimentacao;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.TipoEvidenciaMovimentacao;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.postgresql.util.PSQLException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("postgres-audit")
class BancoAuditoriaPostgresIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private ComprovanteMovimentacaoRepository comprovanteRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EvidenciaMovimentacaoRepository evidenciaRepository;

    @Test
    void updateDiretoEmMovimentacaoFinalizadaEBloqueadoPeloPostgres() {
        Movimentacao salva = criarMovimentacaoFinalizada();

        Integer quantidadeOriginal = jdbcTemplate.queryForObject(
                "SELECT quantidade FROM tb_movimentacoes WHERE id = ?",
                Integer.class,
                salva.getId()
        );
        assertEquals(2, quantidadeOriginal);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        "UPDATE tb_movimentacoes SET quantidade = 999 WHERE id = ?",
                        salva.getId()
                )
        );

        assertBloqueioDoTrigger(
                exception,
                "Dados críticos de uma movimentação finalizada não podem ser alterados"
        );

        Integer quantidadeDepois = jdbcTemplate.queryForObject(
                "SELECT quantidade FROM tb_movimentacoes WHERE id = ?",
                Integer.class,
                salva.getId()
        );
        assertEquals(quantidadeOriginal, quantidadeDepois);
    }

    @Test
    void deleteDiretoDeMovimentacaoFinalizadaEBloqueadoPeloPostgres() {
        // Arrange
        Movimentacao salva = criarMovimentacaoFinalizada();
        Long registrosAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE id = ?",
                Long.class,
                salva.getId()
        );
        assertEquals(1L, registrosAntes);

        // Act
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        "DELETE FROM tb_movimentacoes WHERE id = ?",
                        salva.getId()
                )
        );

        // Assert
        assertBloqueioDoTrigger(
                exception,
                "Movimentações finalizadas não podem ser excluídas"
        );

        Long registrosDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE id = ?",
                Long.class,
                salva.getId()
        );
        assertEquals(1L, registrosDepois);
    }

    @Test
    void updateDiretoEmComprovanteEBloqueadoPeloPostgres() {
        // Arrange
        ComprovanteMovimentacao comprovante = criarComprovanteDeMovimentacaoFinalizada();
        String nomeOriginal = jdbcTemplate.queryForObject(
                "SELECT material_nome FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                String.class,
                comprovante.getMovimentacaoId()
        );
        assertEquals(comprovante.getMaterialNome(), nomeOriginal);

        // Act
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        "UPDATE tb_comprovantes_movimentacao SET material_nome = ? WHERE movimentacao_id = ?",
                        "Material adulterado",
                        comprovante.getMovimentacaoId()
                )
        );

        // Assert
        assertBloqueioDoTrigger(
                exception,
                "Registros de comprovante e evidência são imutáveis"
        );
        String nomeDepois = jdbcTemplate.queryForObject(
                "SELECT material_nome FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                String.class,
                comprovante.getMovimentacaoId()
        );
        assertEquals(nomeOriginal, nomeDepois);
    }

    @Test
    void deleteDiretoDeComprovanteEBloqueadoPeloPostgres() {
        // Arrange
        ComprovanteMovimentacao comprovante = criarComprovanteDeMovimentacaoFinalizada();
        Long registrosAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                Long.class,
                comprovante.getMovimentacaoId()
        );
        assertEquals(1L, registrosAntes);

        // Act
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        "DELETE FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                        comprovante.getMovimentacaoId()
                )
        );

        // Assert
        assertBloqueioDoTrigger(
                exception,
                "Registros de comprovante e evidência são imutáveis"
        );
        Long registrosDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                Long.class,
                comprovante.getMovimentacaoId()
        );
        assertEquals(1L, registrosDepois);
    }

    @Test
    void updateDiretoEmEvidenciaEBloqueadoPeloPostgres() throws NoSuchAlgorithmException {
        // Arrange
        EvidenciaMovimentacao evidencia = criarEvidenciaDeMovimentacaoFinalizada();
        String nomeOriginal = jdbcTemplate.queryForObject(
                "SELECT nome_arquivo_original FROM tb_evidencias_movimentacao WHERE id = ?",
                String.class,
                evidencia.getId()
        );
        assertEquals(evidencia.getNomeArquivoOriginal(), nomeOriginal);

        // Act
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        "UPDATE tb_evidencias_movimentacao SET nome_arquivo_original = ? WHERE id = ?",
                        "assinatura-adulterada.png",
                        evidencia.getId()
                )
        );

        // Assert
        assertBloqueioDoTrigger(
                exception,
                "Registros de comprovante e evidência são imutáveis"
        );
        String nomeDepois = jdbcTemplate.queryForObject(
                "SELECT nome_arquivo_original FROM tb_evidencias_movimentacao WHERE id = ?",
                String.class,
                evidencia.getId()
        );
        assertEquals(nomeOriginal, nomeDepois);
    }

    @Test
    void deleteDiretoDeEvidenciaEBloqueadoPeloPostgres() throws NoSuchAlgorithmException {
        // Arrange
        EvidenciaMovimentacao evidencia = criarEvidenciaDeMovimentacaoFinalizada();
        Long registrosAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_evidencias_movimentacao WHERE id = ?",
                Long.class,
                evidencia.getId()
        );
        assertEquals(1L, registrosAntes);

        // Act
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        "DELETE FROM tb_evidencias_movimentacao WHERE id = ?",
                        evidencia.getId()
                )
        );

        // Assert
        assertBloqueioDoTrigger(
                exception,
                "Registros de comprovante e evidência são imutáveis"
        );
        Long registrosDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_evidencias_movimentacao WHERE id = ?",
                Long.class,
                evidencia.getId()
        );
        assertEquals(1L, registrosDepois);
    }

    @Test
    void mesmoUsuarioEMesmaIdempotencyKeyNaoPodemExistirDuasVezesNoPostgres()
            throws NoSuchAlgorithmException {
        // Arrange
        Usuario usuario = criarUsuarioDeAuditoria();
        String chave = "teste-chave-001";
        String fingerprint = sha256(chave.getBytes(StandardCharsets.UTF_8));
        Movimentacao primeira = criarMovimentacaoFinalizada(
                TipoMovimentacao.ENTRADA, null, null, usuario, chave, fingerprint
        );
        Long registrosAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE usuario_id = ? AND idempotency_key = ?",
                Long.class,
                usuario.getId(),
                chave
        );
        assertEquals(1L, registrosAntes);
        String fingerprintSalvo = jdbcTemplate.queryForObject(
                "SELECT request_fingerprint FROM tb_movimentacoes WHERE id = ?",
                String.class,
                primeira.getId()
        );
        assertEquals(fingerprint, fingerprintSalvo);

        // Act
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inserirMovimentacaoDiretamente(
                        primeira.getMaterial().getId(), usuario.getId(), chave, fingerprint
                )
        );

        // Assert
        assertViolacaoDeConstraint(
                exception, "23505", "uk_movimentacao_usuario_idempotency"
        );
        Long registrosDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE usuario_id = ? AND idempotency_key = ?",
                Long.class,
                usuario.getId(),
                chave
        );
        assertEquals(1L, registrosDepois);
    }

    @Test
    void usuariosDiferentesPodemUsarMesmaIdempotencyKey() throws NoSuchAlgorithmException {
        // Arrange
        Usuario usuarioA = criarUsuarioDeAuditoria();
        Usuario usuarioB = criarUsuarioDeAuditoria();
        Material material = criarMaterialDeAuditoria();
        String chave = "teste-chave-001";
        String fingerprint = sha256(chave.getBytes(StandardCharsets.UTF_8));

        // Act
        assertEquals(1, inserirMovimentacaoDiretamente(
                material.getId(), usuarioA.getId(), chave, fingerprint
        ));
        assertEquals(1, inserirMovimentacaoDiretamente(
                material.getId(), usuarioB.getId(), chave, fingerprint
        ));

        // Assert
        Long registrosDoUsuarioA = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE usuario_id = ? AND idempotency_key = ?",
                Long.class,
                usuarioA.getId(),
                chave
        );
        Long registrosDoUsuarioB = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE usuario_id = ? AND idempotency_key = ?",
                Long.class,
                usuarioB.getId(),
                chave
        );
        assertEquals(1L, registrosDoUsuarioA);
        assertEquals(1L, registrosDoUsuarioB);
    }

    @Test
    void inserirMovimentacaoComMaterialInexistenteEFalhaPorForeignKey() {
        // Arrange
        Long materialInexistente = idInexistente("tb_materiais");
        Long registrosAntes = contarMovimentacoes();

        // Act
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inserirMovimentacaoComReferencias(
                        null, null, materialInexistente, null, null
                )
        );

        // Assert
        assertViolacaoDeConstraint(exception, "23503", "fk_movimentacoes_material");
        assertEquals(registrosAntes, contarMovimentacoes());
    }

    @ParameterizedTest(name = "{0} rejeita ID inexistente")
    @CsvSource({
            "funcionario_id, tb_funcionarios, fk_movimentacoes_funcionario",
            "contrato_id, tb_contratos, fk_movimentacoes_contrato",
            "usuario_id, tb_usuarios, fk_movimentacoes_usuario",
            "nota_fiscal_id, tb_notas_fiscais, fk_movimentacoes_nota_fiscal"
    })
    void inserirMovimentacaoComReferenciaInexistenteEFalhaPorForeignKey(
            String coluna, String tabelaPai, String constraint
    ) {
        // Arrange
        Material material = criarMaterialDeAuditoria();
        Long paiInexistente = idInexistente(tabelaPai);
        Long registrosAntes = contarMovimentacoes();

        // Act
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inserirMovimentacaoComReferencias(
                        coluna.equals("funcionario_id") ? paiInexistente : null,
                        coluna.equals("contrato_id") ? paiInexistente : null,
                        material.getId(),
                        coluna.equals("usuario_id") ? paiInexistente : null,
                        coluna.equals("nota_fiscal_id") ? paiInexistente : null
                )
        );

        // Assert
        assertViolacaoDeConstraint(exception, "23503", constraint);
        assertEquals(registrosAntes, contarMovimentacoes());
    }

    @Test
    void comprovanteNaoPodeReferenciarMovimentacaoInexistente() {
        // Arrange
        Long movimentacaoInexistente = idInexistente("tb_movimentacoes");
        Material material = criarMaterialDeAuditoria();
        Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
        Long registrosAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                Long.class,
                movimentacaoInexistente
        );
        assertEquals(0L, registrosAntes);

        // Act
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO tb_comprovantes_movimentacao (
                            movimentacao_id, tipo, quantidade, data_movimentacao,
                            data_finalizacao, material_id, material_nome,
                            material_descricao, gerado_em, versao
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        movimentacaoInexistente,
                        TipoMovimentacao.ENTRADA.name(),
                        2,
                        agora,
                        agora,
                        material.getId(),
                        material.getNome(),
                        material.getDescricao(),
                        agora,
                        1
                )
        );

        // Assert
        assertViolacaoDeConstraint(exception, "23503", "fk_comprovantes_movimentacao");
        Long registrosDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_comprovantes_movimentacao WHERE movimentacao_id = ?",
                Long.class,
                movimentacaoInexistente
        );
        assertEquals(0L, registrosDepois);
    }

    @Test
    void evidenciaNaoPodeReferenciarMovimentacaoInexistente()
            throws NoSuchAlgorithmException {
        // Arrange
        Long movimentacaoInexistente = idInexistente("tb_movimentacoes");
        Funcionario funcionario = criarFuncionarioDeAuditoria();
        Usuario usuario = criarUsuarioDeAuditoria();
        byte[] assinaturaPng = assinaturaPng();
        String hashAssinatura = sha256(assinaturaPng);
        String storageKey = "movimentacoes/" + movimentacaoInexistente
                + "/assinatura/" + UUID.randomUUID() + ".png";
        Long registrosAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_evidencias_movimentacao WHERE movimentacao_id = ?",
                Long.class,
                movimentacaoInexistente
        );
        assertEquals(0L, registrosAntes);

        // Act
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO tb_evidencias_movimentacao (
                            movimentacao_id, tipo, data_evidencia, funcionario_id,
                            funcionario_nome, registrada_por_id, registrada_por_username,
                            storage_key, nome_arquivo_original, content_type,
                            tamanho_bytes, sha256
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        movimentacaoInexistente,
                        TipoEvidenciaMovimentacao.ASSINATURA.name(),
                        Timestamp.valueOf(LocalDateTime.now()),
                        funcionario.getId(),
                        funcionario.getNome(),
                        usuario.getId(),
                        usuario.getUsername(),
                        storageKey,
                        "assinatura.png",
                        "image/png",
                        (long) assinaturaPng.length,
                        hashAssinatura
                )
        );

        // Assert
        assertViolacaoDeConstraint(exception, "23503", "fk_evidencias_movimentacao");
        Long registrosDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_evidencias_movimentacao WHERE movimentacao_id = ?",
                Long.class,
                movimentacaoInexistente
        );
        assertEquals(0L, registrosDepois);
    }

    @Test
    void materialReferenciadoNaoPodeSerExcluidoDiretamente() {
        // Arrange
        Material material = criarMaterialDeAuditoria();
        assertEquals(1, inserirMovimentacaoComReferencias(
                null, null, material.getId(), null, null
        ));
        Long movimentacoesAntes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE material_id = ?",
                Long.class,
                material.getId()
        );
        assertEquals(1L, movimentacoesAntes);

        // Act
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("DELETE FROM tb_materiais WHERE id = ?", material.getId())
        );

        // Assert
        assertViolacaoDeConstraint(exception, "23503", "fk_movimentacoes_material");
        Long materiaisDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_materiais WHERE id = ?",
                Long.class,
                material.getId()
        );
        Long movimentacoesDepois = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes WHERE material_id = ?",
                Long.class,
                material.getId()
        );
        assertEquals(1L, materiaisDepois);
        assertEquals(movimentacoesAntes, movimentacoesDepois);
    }

    private int inserirMovimentacaoDiretamente(
            Long materialId, Long usuarioId, String chave, String fingerprint
    ) {
        Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
        return jdbcTemplate.update(
                """
                INSERT INTO tb_movimentacoes (
                    material_id, quantidade, tipo, data_movimentacao, data_finalizacao,
                    usuario_id, idempotency_key, request_fingerprint
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                materialId,
                2,
                TipoMovimentacao.ENTRADA.name(),
                agora,
                agora,
                usuarioId,
                chave,
                fingerprint
        );
    }

    private int inserirMovimentacaoComReferencias(
            Long funcionarioId, Long contratoId, Long materialId,
            Long usuarioId, Long notaFiscalId
    ) {
        Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
        return jdbcTemplate.update(
                """
                INSERT INTO tb_movimentacoes (
                    funcionario_id, contrato_id, material_id, usuario_id,
                    nota_fiscal_id, quantidade, tipo, data_movimentacao, data_finalizacao
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                funcionarioId,
                contratoId,
                materialId,
                usuarioId,
                notaFiscalId,
                2,
                TipoMovimentacao.ENTRADA.name(),
                agora,
                agora
        );
    }

    private Long contarMovimentacoes() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes", Long.class
        );
    }

    private Long idInexistente(String tabela) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM " + tabela, Long.class
        );
        Long registros = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tabela + " WHERE id = ?", Long.class, id
        );
        assertEquals(0L, registros);
        return id;
    }

    private void assertViolacaoDeConstraint(
            DataIntegrityViolationException exception, String sqlState, String constraint
    ) {
        PSQLException causaPostgres = assertInstanceOf(
                PSQLException.class,
                exception.getMostSpecificCause()
        );
        assertEquals(sqlState, causaPostgres.getSQLState());
        assertNotNull(causaPostgres.getServerErrorMessage());
        assertEquals(constraint, causaPostgres.getServerErrorMessage().getConstraint());
    }

    private EvidenciaMovimentacao criarEvidenciaDeMovimentacaoFinalizada()
            throws NoSuchAlgorithmException {
        Funcionario funcionario = criarFuncionarioDeAuditoria();
        Contrato contrato = contratoRepository.save(
                new Contrato("Contrato-" + UUID.randomUUID(), "Contrato de auditoria", true)
        );
        Usuario usuario = criarUsuarioDeAuditoria();
        Movimentacao movimentacao = criarMovimentacaoFinalizada(
                TipoMovimentacao.RETIRADA, funcionario, contrato, usuario
        );

        byte[] assinaturaPng = assinaturaPng();
        String sha256 = sha256(assinaturaPng);
        String storageKey = "movimentacoes/" + movimentacao.getId()
                + "/assinatura/" + UUID.randomUUID() + ".png";
        return evidenciaRepository.saveAndFlush(new EvidenciaMovimentacao(
                movimentacao.getId(),
                TipoEvidenciaMovimentacao.ASSINATURA,
                LocalDateTime.now(),
                funcionario.getId(),
                funcionario.getNome(),
                usuario.getId(),
                usuario.getUsername(),
                storageKey,
                "assinatura.png",
                "image/png",
                (long) assinaturaPng.length,
                sha256
        ));
    }

    private ComprovanteMovimentacao criarComprovanteDeMovimentacaoFinalizada() {
        Movimentacao movimentacao = criarMovimentacaoFinalizada();
        return comprovanteRepository.saveAndFlush(ComprovanteMovimentacao.registrar(movimentacao));
    }

    private Usuario criarUsuarioDeAuditoria() {
        return usuarioRepository.save(
                TestUsuarioFactory.usuarioPersistivel(
                        "auditoria-" + UUID.randomUUID(),
                        "senha-de-teste",
                        Role.OPERADOR,
                        true
                )
        );
    }

    private Funcionario criarFuncionarioDeAuditoria() {
        return funcionarioRepository.save(
                new Funcionario(
                        "Funcionário de auditoria",
                        String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L)),
                        "Técnico"
                )
        );
    }

    private Material criarMaterialDeAuditoria() {
        return materialRepository.save(
                new Material("Capacete-" + UUID.randomUUID(), "Material de teste", 10)
        );
    }

    private byte[] assinaturaPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg=="
        );
    }

    private String sha256(byte[] conteudo) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
    }

    private void assertBloqueioDoTrigger(DataAccessException exception, String mensagemEsperada) {
        SQLException causaPostgres = assertInstanceOf(
                SQLException.class,
                exception.getMostSpecificCause()
        );
        assertEquals("55000", causaPostgres.getSQLState());
        assertTrue(causaPostgres.getMessage().contains(mensagemEsperada));
    }

    private Movimentacao criarMovimentacaoFinalizada() {
        return criarMovimentacaoFinalizada(TipoMovimentacao.ENTRADA, null, null, null);
    }

    private Movimentacao criarMovimentacaoFinalizada(
            TipoMovimentacao tipo,
            Funcionario funcionario,
            Contrato contrato,
            Usuario usuario
    ) {
        return criarMovimentacaoFinalizada(tipo, funcionario, contrato, usuario, null, null);
    }

    private Movimentacao criarMovimentacaoFinalizada(
            TipoMovimentacao tipo,
            Funcionario funcionario,
            Contrato contrato,
            Usuario usuario,
            String chave,
            String fingerprint
    ) {
        Material material = criarMaterialDeAuditoria();

        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setFuncionario(funcionario);
        movimentacao.setContrato(contrato);
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(2);
        movimentacao.setTipo(tipo);
        movimentacao.setRegistradoPor(usuario);
        movimentacao.setIdempotencyKey(chave);
        movimentacao.setRequestFingerprint(fingerprint);
        LocalDateTime agora = LocalDateTime.now();
        movimentacao.setDataMovimentacao(agora);
        movimentacao.setDataFinalizacao(agora);
        return movimentacaoRepository.saveAndFlush(movimentacao);
    }
}
