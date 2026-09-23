package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataInativacaoIntegrationTests {

    private static final String SENHA_ADMIN = "senhaAdmin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private UsuarioService usuarioService;

    private String adminToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        movimentacaoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratoRepository.deleteAll();
        materialRepository.deleteAll();
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(
                usuarioService,
                "admin",
                SENHA_ADMIN,
                Role.ADMIN,
                true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService,
                "operador",
                "senhaOperador123",
                Role.OPERADOR,
                true
        );
        adminToken = token("admin", SENHA_ADMIN);
    }

    @Test
    void responsesDeRegistrosAtivosPossuemDataInativacaoNula()
            throws Exception {
        Usuario usuario = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();
        Contrato contrato = criarContrato();

        validarDataNulaNoResponse("/usuarios/{id}", usuario.getId());
        validarDataNulaNoResponse("/contratos/{id}", contrato.getId());
    }

    @Test
    void usuarioRegistraPreservaLimpaERegistraNovaData() throws Exception {
        Usuario usuario = usuarioRepository
                .findByUsernameIgnoreCase("operador")
                .orElseThrow();
        LocalDateTime antes = LocalDateTime.now();

        MvcResult desativacao = mockMvc.perform(
                        patch("/usuarios/{id}/desativar", usuario.getId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        LocalDateTime depois = LocalDateTime.now();
        Usuario inativo = buscarUsuario(usuario.getId());
        LocalDateTime dataOriginal = inativo.getDataInativacao();
        assertTimestampEntre(dataOriginal, antes, depois);
        assertNotNull(jsonResponse(desativacao).get("dataInativacao").textValue());

        mockMvc.perform(patch("/usuarios/{id}/desativar", usuario.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict());
        assertEquals(dataOriginal, buscarUsuario(usuario.getId()).getDataInativacao());

        mockMvc.perform(put("/usuarios/{id}", usuario.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "operador_editado",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isOk());
        assertEquals(dataOriginal, buscarUsuario(usuario.getId()).getDataInativacao());

        mockMvc.perform(patch("/usuarios/{id}/ativar", usuario.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        assertNull(buscarUsuario(usuario.getId()).getDataInativacao());

        mockMvc.perform(patch("/usuarios/{id}/desativar", usuario.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        LocalDateTime novaData = buscarUsuario(usuario.getId()).getDataInativacao();
        assertNotNull(novaData);
        assertTrue(novaData.isAfter(dataOriginal));
    }

    @Test
    void contratoUsaEndpointsDeStatusEPreservaHistoricoEEdicao()
            throws Exception {
        Contrato contrato = criarContrato();
        Movimentacao movimentacao = criarMovimentacao(contrato);
        LocalDateTime antes = LocalDateTime.now();

        mockMvc.perform(patch("/contratos/{id}/desativar", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        LocalDateTime depois = LocalDateTime.now();
        LocalDateTime dataOriginal = buscarContrato(contrato.getId())
                .getDataInativacao();
        assertTimestampEntre(dataOriginal, antes, depois);

        mockMvc.perform(patch("/contratos/{id}/desativar", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        assertEquals(
                dataOriginal,
                buscarContrato(contrato.getId()).getDataInativacao()
        );

        mockMvc.perform(put("/contratos/{id}", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Contrato Atualizado",
                                "descricao", "DescriÃ§Ã£o atualizada",
                                "ativo", false
                        ))))
                .andExpect(status().isOk());
        Contrato editado = buscarContrato(contrato.getId());
        assertTrue(!Boolean.TRUE.equals(editado.getAtivo()));
        assertEquals(dataOriginal, editado.getDataInativacao());
        assertTrue(movimentacaoRepository.existsById(movimentacao.getId()));

        mockMvc.perform(patch("/contratos/{id}/ativar", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        assertNull(buscarContrato(contrato.getId()).getDataInativacao());
        assertTrue(movimentacaoRepository.existsById(movimentacao.getId()));

        mockMvc.perform(put("/contratos/{id}", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Contrato Atualizado",
                                "descricao", "DescriÃ§Ã£o atualizada",
                                "ativo", false
                        ))))
                .andExpect(status().isOk());
        assertNotNull(buscarContrato(contrato.getId()).getDataInativacao());

        mockMvc.perform(put("/contratos/{id}", contrato.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", "Contrato Atualizado",
                                "descricao", "DescriÃ§Ã£o atualizada",
                                "ativo", true
                        ))))
                .andExpect(status().isOk());
        assertNull(buscarContrato(contrato.getId()).getDataInativacao());
        assertTrue(movimentacaoRepository.existsById(movimentacao.getId()));
    }

    private void validarDataNulaNoResponse(String url, Long id) throws Exception {
        MvcResult result = mockMvc.perform(get(url, id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = jsonResponse(result);
        assertTrue(response.has("dataInativacao"));
        assertTrue(response.get("dataInativacao").isNull());
    }

    private Funcionario criarFuncionario() {
        return funcionarioRepository.save(
                new Funcionario("Maria Silva", "12345678909", "Arquiteta")
        );
    }

    private Contrato criarContrato() {
        return contratoRepository.save(
                new Contrato("Contrato A", "DescriÃ§Ã£o", true)
        );
    }

    private Movimentacao criarMovimentacao(Contrato contrato) {
        Funcionario funcionario = criarFuncionario();
        Material material = materialRepository.save(
                new Material("Capacete", "ProteÃ§Ã£o", 10)
        );
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setFuncionario(funcionario);
        movimentacao.setContrato(contrato);
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(1);
        movimentacao.setTipo(TipoMovimentacao.RETIRADA);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setRegistradoPor(
                usuarioRepository.findByUsernameIgnoreCase("admin").orElseThrow()
        );
        return movimentacaoRepository.save(movimentacao);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow();
    }

    private Contrato buscarContrato(Long id) {
        return contratoRepository.findById(id).orElseThrow();
    }

    private void assertTimestampEntre(
            LocalDateTime valor,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        assertNotNull(valor);
        assertTrue(!valor.isBefore(inicio.minusSeconds(1)));
        assertTrue(!valor.isAfter(fim.plusSeconds(1)));
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return jsonResponse(result).get("token").asText();
    }

    private JsonNode jsonResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
