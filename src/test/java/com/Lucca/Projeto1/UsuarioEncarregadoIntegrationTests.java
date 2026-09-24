package com.Lucca.Projeto1;

import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UsuarioEncarregadoIntegrationTests {

    private static final String SENHA = "senhaForte123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String operadorToken;
    private String gerenteToken;
    private String encarregadoToken;

    @BeforeEach
    void prepararUsuarios() throws Exception {
        usuarioRepository.deleteAll();

        TestUsuarioFactory.criarUsuario(
                usuarioService, "admin", SENHA, Role.ADMIN, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService, "operador", SENHA, Role.OPERADOR, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService, "gerente", SENHA, Role.GERENTE, true
        );
        TestUsuarioFactory.criarUsuario(
                usuarioService, "encarregado", SENHA, Role.ENCARREGADO, true
        );

        adminToken = token("admin");
        operadorToken = token("operador");
        gerenteToken = token("gerente");
        encarregadoToken = token("encarregado");
    }

    @Test
    void somenteAsQuatroRolesFinaisExistemESaoAceitas() throws Exception {
        assertEquals(
                List.of(
                        Role.ADMIN,
                        Role.OPERADOR,
                        Role.GERENTE,
                        Role.ENCARREGADO
                ),
                Arrays.asList(Role.values())
        );
        assertThrows(IllegalArgumentException.class, () -> Role.valueOf("CONSULTA"));
        assertThrows(IllegalArgumentException.class, () -> Role.valueOf("OFICIAL"));

        for (String roleInexistente : List.of("CONSULTA", "OFICIAL")) {
            Map<String, Object> request = criarUsuarioPayload(
                    "Inválido " + roleInexistente,
                    TestUsuarioFactory.proximoCpf(),
                    "11988887777",
                    "invalido-" + roleInexistente.toLowerCase(),
                    roleInexistente
            );

            mockMvc.perform(post("/usuarios")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void adminEGerentePodemCriarEncarregado() throws Exception {
        criarEncarregado(adminToken, "enc-admin", TestUsuarioFactory.proximoCpf())
                .andExpect(status().isCreated());
        criarEncarregado(gerenteToken, "enc-gerente", TestUsuarioFactory.proximoCpf())
                .andExpect(status().isCreated());

        assertEquals(
                Role.ENCARREGADO,
                usuarioRepository.findByUsernameIgnoreCase("enc-admin")
                        .orElseThrow()
                        .getRole()
        );
        assertEquals(
                Role.ENCARREGADO,
                usuarioRepository.findByUsernameIgnoreCase("enc-gerente")
                        .orElseThrow()
                        .getRole()
        );
    }

    @Test
    void operadorEEncarregadoNaoPodemCriarEncarregado() throws Exception {
        criarEncarregado(operadorToken, "negado-operador", TestUsuarioFactory.proximoCpf())
                .andExpect(status().isForbidden());
        criarEncarregado(encarregadoToken, "negado-enc", TestUsuarioFactory.proximoCpf())
                .andExpect(status().isForbidden());

        assertFalse(usuarioRepository.existsByUsernameIgnoreCase("negado-operador"));
        assertFalse(usuarioRepository.existsByUsernameIgnoreCase("negado-enc"));
    }

    @Test
    void criacaoForcaRoleNormalizaDadosUsaBCryptENaoExpoeDadosSensiveis()
            throws Exception {
        Map<String, Object> request = encarregadoPayload(
                "  Encarregado Novo  ",
                "123.456.789-09",
                "(11) 98765-4321",
                "  encarregado.novo  "
        );
        request.put("role", "ADMIN");

        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("encarregado.novo"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario criado = usuarioRepository
                .findByUsernameIgnoreCase("encarregado.novo")
                .orElseThrow();

        assertEquals("Encarregado Novo", criado.getNome());
        assertEquals("12345678909", criado.getCpf());
        assertEquals("11987654321", criado.getCelular());
        assertEquals(Role.ENCARREGADO, criado.getRole());
        assertFalse(SENHA.equals(criado.getSenha()));
        assertTrue(passwordEncoder.matches(SENHA, criado.getSenha()));
    }

    @Test
    void gerenteEditaEAlteraStatusApenasDeEncarregado() throws Exception {
        Usuario encarregado = usuarioRepository.findByUsernameIgnoreCase("encarregado").orElseThrow();
        Usuario admin = usuarioRepository.findByUsernameIgnoreCase("admin").orElseThrow();

        mockMvc.perform(put("/usuarios/encarregados/{id}", encarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "encarregado-editado", "novaSenha", "novaSenha123", "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("encarregado-editado"));

        assertEquals(Role.ENCARREGADO, usuarioRepository.findById(encarregado.getId()).orElseThrow().getRole());
        assertTrue(passwordEncoder.matches("novaSenha123", usuarioRepository.findById(encarregado.getId()).orElseThrow().getSenha()));

        mockMvc.perform(patch("/usuarios/encarregados/{id}/desativar", encarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mockMvc.perform(patch("/usuarios/encarregados/{id}/ativar", encarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));

        mockMvc.perform(put("/usuarios/encarregados/{id}", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin-alterado"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/usuarios/encarregados/{id}/desativar", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerenteNaoPodeUsarAdministracaoGeralParaCriarOutrosPerfis() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(criarUsuarioPayload(
                                "Admin indevido", TestUsuarioFactory.proximoCpf(), "11988887777", "admin-indevido", "ADMIN"
                        ))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(criarUsuarioPayload(
                                "Operador indevido", TestUsuarioFactory.proximoCpf(), "11988887778", "operador-indevido", "OPERADOR"
                        ))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(criarUsuarioPayload(
                                "Gerente indevido", TestUsuarioFactory.proximoCpf(), "11988887779", "gerente-indevido", "GERENTE"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cpfContinuaUnicoAposNormalizacao() throws Exception {
        criarEncarregado(adminToken, "primeiro-cpf", "123.456.789-09")
                .andExpect(status().isCreated());
        criarEncarregado(adminToken, "segundo-cpf", "12345678909")
                .andExpect(status().isConflict());

        assertFalse(usuarioRepository.existsByUsernameIgnoreCase("segundo-cpf"));
    }

    @Test
    void usernameContinuaUnicoIgnorandoCaixaEEspacos() throws Exception {
        criarEncarregado(adminToken, "usuario-unico", TestUsuarioFactory.proximoCpf())
                .andExpect(status().isCreated());

        Map<String, Object> request = encarregadoPayload(
                "Outro Encarregado",
                TestUsuarioFactory.proximoCpf(),
                "11988887777",
                "  USUARIO-UNICO  "
        );

        mockMvc.perform(post("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listagemDeEncarregadosAutorizaAdminOperadorEGerenteSemExporCpfOuSenha()
            throws Exception {
        for (String token : List.of(adminToken, operadorToken, gerenteToken)) {
            mockMvc.perform(get("/usuarios/encarregados")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").isNumber())
                    .andExpect(jsonPath("$[0].nome").isString())
                    .andExpect(jsonPath("$[0].celular").isString())
                    .andExpect(jsonPath("$[0].username").value("encarregado"))
                    .andExpect(jsonPath("$[0].ativo").value(true))
                    .andExpect(jsonPath("$[0].cpf").doesNotExist())
                    .andExpect(jsonPath("$[0].password").doesNotExist())
                    .andExpect(jsonPath("$[0].senha").doesNotExist());
        }
    }

    @Test
    void encarregadoNaoPodeListarTodosOsEncarregados() throws Exception {
        mockMvc.perform(get("/usuarios/encarregados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(encarregadoToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void responseAdministrativoComumNaoExpoeCpfNemSenha() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").isString())
                .andExpect(jsonPath("$[0].celular").isString())
                .andExpect(jsonPath("$[0].cpf").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].senha").doesNotExist());
    }

    @Test
    void somenteAdminMantemAcessoAAdministracaoGeralDeUsuarios()
            throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        for (String token : List.of(operadorToken, gerenteToken, encarregadoToken)) {
            mockMvc.perform(get("/usuarios")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void ultimoAdminAtivoNaoPodeSerDesativadoNemPerderRole() throws Exception {
        Usuario admin = usuarioRepository
                .findByUsernameIgnoreCase("admin")
                .orElseThrow();

        assertThrows(
                RegraNegocioException.class,
                () -> usuarioService.desativar(admin.getId(), "outro-admin")
        );

        mockMvc.perform(put("/usuarios/{id}", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "role", "OPERADOR"
                        ))))
                .andExpect(status().isConflict());

        assertEquals(
                Role.ADMIN,
                usuarioRepository.findById(admin.getId()).orElseThrow().getRole()
        );
    }

    @Test
    void ativacaoEDesativacaoContinuamDisponiveisSomenteParaAdmin()
            throws Exception {
        Usuario encarregado = usuarioRepository
                .findByUsernameIgnoreCase("encarregado")
                .orElseThrow();

        mockMvc.perform(patch("/usuarios/{id}/desativar", encarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.cpf").doesNotExist());

        assertTrue(usuarioRepository.findById(encarregado.getId())
                .orElseThrow()
                .getDataInativacao() != null);

        mockMvc.perform(patch("/usuarios/{id}/ativar", encarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerenteToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/usuarios/{id}/ativar", encarregado.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions criarEncarregado(
            String token,
            String username,
            String cpf
    ) throws Exception {
        return mockMvc.perform(post("/usuarios/encarregados")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(encarregadoPayload(
                        "Encarregado " + username,
                        cpf,
                        "(11) 98888-7777",
                        username
                ))));
    }

    private Map<String, Object> encarregadoPayload(
            String nome,
            String cpf,
            String celular,
            String username
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("nome", nome);
        request.put("cpf", cpf);
        request.put("celular", celular);
        request.put("username", username);
        request.put("password", SENHA);
        return request;
    }

    private Map<String, Object> criarUsuarioPayload(
            String nome,
            String cpf,
            String celular,
            String username,
            String role
    ) {
        Map<String, Object> request = encarregadoPayload(
                nome,
                cpf,
                celular,
                username
        );
        request.put("role", role);
        return request;
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", SENHA
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
