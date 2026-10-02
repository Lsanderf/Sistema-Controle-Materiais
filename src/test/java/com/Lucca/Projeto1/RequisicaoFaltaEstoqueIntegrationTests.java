package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RequisicaoFaltaEstoqueIntegrationTests {

    private static final String SENHA = "senhaCompra123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private MaterialRepository materialRepository;

    @Test
    void operadorRegistraFaltaParaGerenteSemTransformarEncarregadoEmDestinatario() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario operador = TestUsuarioFactory.criarUsuario(usuarioService, "operador-compra-" + sufixo, SENHA, Role.OPERADOR, true);
        Usuario encarregado = TestUsuarioFactory.criarUsuario(usuarioService, "encarregado-compra-" + sufixo, SENHA, Role.ENCARREGADO, true);
        Usuario gerente = TestUsuarioFactory.criarUsuario(usuarioService, "gerente-compra-" + sufixo, SENHA, Role.GERENTE, true);
        Contrato contrato = contratoRepository.save(new Contrato("Contrato compra " + sufixo, "Teste", true));
        Material material = materialRepository.save(new Material("Material compra " + sufixo, "Teste", 2));

        mockMvc.perform(post("/requisicoes/falta-estoque")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token(operador.getUsername())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "gerenteDestinatarioId", gerente.getId(),
                                "encarregadoNecessidadeId", encarregado.getId(),
                                "contratoId", contrato.getId(),
                                "itens", List.of(Map.of("materialId", material.getId(), "quantidadeSolicitada", 5))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("FALTA_ESTOQUE"))
                .andExpect(jsonPath("$.operadorRegistrador.id").value(operador.getId()))
                .andExpect(jsonPath("$.encarregadoNecessidade.id").value(encarregado.getId()))
                .andExpect(jsonPath("$.gerenteDestinatario.id").value(gerente.getId()))
                .andExpect(jsonPath("$.itens[0].quantidadeSolicitada").value(5))
                .andExpect(jsonPath("$.itens[0].quantidadeDisponivel").value(2))
                .andExpect(jsonPath("$.itens[0].quantidadeFaltante").value(3));
    }

    private String token(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", SENHA))))
                .andExpect(status().isOk()).andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
    private String bearer(String token) { return "Bearer " + token; }
}
