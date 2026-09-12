package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import com.Lucca.Projeto1.validation.ValidadorChaveAcessoNfe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ImportacaoXmlNfeIntegrationTests {

    private static final String SENHA = "senhaOperador123";
    private static final String SENHA_CONSULTA = "senhaConsulta123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private NotaFiscalEntradaRepository notaFiscalRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    private String operadorToken;
    private String consultaToken;

    @BeforeEach
    void prepararBanco() throws Exception {
        movimentacaoRepository.deleteAll();
        notaFiscalRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.criarUsuario(
                "operador",
                SENHA,
                Role.OPERADOR,
                true
        );
        usuarioService.criarUsuario(
                "consulta",
                SENHA_CONSULTA,
                Role.CONSULTA,
                true
        );

        operadorToken = token("operador", SENHA);
        consultaToken = token("consulta", SENHA_CONSULTA);
    }

    @Test
    void xmlValidoExtraiDadosEValoresSemPersistirNota() throws Exception {
        String chave = chave(100);

        mockMvc.perform(requisicaoImportacao(xmlValido(chave, "2.0000"))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chaveAcesso").value(chave))
                .andExpect(jsonPath("$.numero").value("12345"))
                .andExpect(jsonPath("$.serie").value("1"))
                .andExpect(jsonPath("$.dataEmissao").value("2026-09-10"))
                .andExpect(jsonPath("$.cnpjFornecedor")
                        .value("11222333000181"))
                .andExpect(jsonPath("$.fornecedor")
                        .value("ABC Materiais Ltda"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].numeroItem").value(1))
                .andExpect(jsonPath("$.itens[0].codigoProduto")
                        .value("82731"))
                .andExpect(jsonPath("$.itens[0].descricaoProduto")
                        .value("PARAFUSO SEXTAVADO 10MM"))
                .andExpect(jsonPath("$.itens[0].quantidadeComercial")
                        .value("2"))
                .andExpect(jsonPath("$.itens[0].unidadeComercial")
                        .value("UN"))
                .andExpect(jsonPath("$.itens[0].valorUnitarioComercial")
                        .value("12.5"))
                .andExpect(jsonPath("$.itens[0].valorTotal")
                        .value("25"))
                .andExpect(jsonPath("$.itens[0].eanGtin")
                        .value("7891234567895"));

        assertEquals(0, notaFiscalRepository.count());
        assertEquals(0, movimentacaoRepository.count());
    }

    @Test
    void camposOpcionaisAusentesSaoAceitos() throws Exception {
        String xml = xmlValido(chave(101), "1")
                .replace("<cEAN>7891234567895</cEAN>", "")
                .replace("<vProd>25.00</vProd>", "");

        mockMvc.perform(requisicaoImportacao(xml)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].eanGtin").doesNotExist())
                .andExpect(jsonPath("$.itens[0].valorTotal").doesNotExist());
    }

    @Test
    void xmlVazioMalformadoEArquivoNaoNfeSaoRejeitados() throws Exception {
        importarComErro("", "está vazio");
        importarComErro("<NFe>", "malformado");
        importarComErro(
                "<pedido xmlns=\"http://www.portalfiscal.inf.br/nfe\"/>",
                "não representa uma NF-e"
        );
    }

    @Test
    void chaveComDvInvalidoNoXmlERejeitada() throws Exception {
        String valida = chave(102);
        int novoDv = (Character.digit(valida.charAt(43), 10) + 1) % 10;
        String invalida = valida.substring(0, 43) + novoDv;

        mockMvc.perform(requisicaoImportacao(xmlValido(invalida, "1"))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Chave de acesso da NF-e inválida. Verifique os números informados."
                ));
    }

    @Test
    void chaveInformadaDiferenteDaChaveDoXmlERejeitada() throws Exception {
        String chaveXml = chave(103);
        String chaveInformada = chave(104);

        mockMvc.perform(requisicaoImportacao(xmlValido(chaveXml, "1"))
                        .param("chaveAcessoInformada", chaveInformada)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "A chave da NF-e informada é diferente da chave presente no XML."
                ));
    }

    @Test
    void chaveInformadaIgualPermiteImportacao() throws Exception {
        String chave = chave(105);

        mockMvc.perform(requisicaoImportacao(xmlValido(chave, "1"))
                        .param("chaveAcessoInformada", formatarChave(chave))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chaveAcesso").value(chave));
    }

    @Test
    void chaveJaCadastradaERejeitadaExcetoNaEdicaoDaPropriaNota()
            throws Exception {
        String chave = chave(106);
        Long notaId = criarRascunho(chave);

        mockMvc.perform(requisicaoImportacao(xmlValido(chave, "1"))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(
                        "Já existe uma nota fiscal com essa chave de acesso"
                ));

        mockMvc.perform(requisicaoImportacao(xmlValido(chave, "1"))
                        .param("notaFiscalId", notaId.toString())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isOk());
    }

    @Test
    void campoObrigatorioAusenteEFormatoNaoSuportadoSaoRejeitados()
            throws Exception {
        String semEmitente = xmlValido(chave(107), "1")
                .replace("<xNome>ABC Materiais Ltda</xNome>", "");
        importarComErro(semEmitente, "xNome é obrigatório");

        String versaoNaoSuportada = xmlValido(chave(108), "1")
                .replace("versao=\"4.00\"", "versao=\"5.00\"");
        importarComErro(versaoNaoSuportada, "não é suportada");
    }

    @Test
    void parserBloqueiaDoctypeEXxe() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE NFe [
                  <!ENTITY xxe SYSTEM "file:///C:/Windows/win.ini">
                ]>
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe%s" versao="4.00">
                    <ide><mod>55</mod></ide>
                    <emit><xNome>&xxe;</xNome></emit>
                  </infNFe>
                </NFe>
                """.formatted(chave(109));

        mockMvc.perform(requisicaoImportacao(xml)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "O arquivo não contém um XML de NF-e válido ou está malformado."
                ));
    }

    @Test
    void quantidadeFracionariaNaoEArredondada() throws Exception {
        mockMvc.perform(requisicaoImportacao(xmlValido(chave(110), "1.5"))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        org.hamcrest.Matchers.containsString(
                                "possui casas decimais"
                        )
                ));
    }

    @Test
    void arquivoAcimaDoLimiteERejeitado() throws Exception {
        byte[] conteudo = new byte[(2 * 1024 * 1024) + 1];
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "grande.xml",
                MediaType.APPLICATION_XML_VALUE,
                conteudo
        );

        mockMvc.perform(multipart("/notas-fiscais/importar-xml")
                        .file(arquivo)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void perfilConsultaNaoPodeImportarXml() throws Exception {
        mockMvc.perform(requisicaoImportacao(xmlValido(chave(111), "1"))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(consultaToken)
                        ))
                .andExpect(status().isForbidden());
    }

    private void importarComErro(String xml, String trechoEsperado)
            throws Exception {
        mockMvc.perform(requisicaoImportacao(xml)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        org.hamcrest.Matchers.containsString(trechoEsperado)
                ));
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
    requisicaoImportacao(String xml) {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "nfe.xml",
                MediaType.APPLICATION_XML_VALUE,
                xml.getBytes(StandardCharsets.UTF_8)
        );
        return multipart("/notas-fiscais/importar-xml").file(arquivo);
    }

    private String xmlValido(String chave, String quantidade) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
                  <NFe>
                    <infNFe Id="NFe%s" versao="4.00">
                      <ide>
                        <mod>55</mod>
                        <serie>1</serie>
                        <nNF>12345</nNF>
                        <dhEmi>2026-09-10T14:30:00-03:00</dhEmi>
                      </ide>
                      <emit>
                        <CNPJ>11222333000181</CNPJ>
                        <xNome>ABC Materiais Ltda</xNome>
                      </emit>
                      <det nItem="1">
                        <prod>
                          <cProd>82731</cProd>
                          <cEAN>7891234567895</cEAN>
                          <xProd>PARAFUSO SEXTAVADO 10MM</xProd>
                          <qCom>%s</qCom>
                          <uCom>UN</uCom>
                          <vUnCom>12.5000000000</vUnCom>
                          <vProd>25.00</vProd>
                        </prod>
                      </det>
                    </infNFe>
                  </NFe>
                </nfeProc>
                """.formatted(chave, quantidade);
    }

    private Long criarRascunho(String chave) throws Exception {
        Map<String, Object> request = Map.of(
                "numero", "12345",
                "serie", "1",
                "chaveAcesso", chave,
                "fornecedor", "ABC Materiais Ltda",
                "cnpjFornecedor", "11.222.333/0001-81",
                "dataEmissao", LocalDate.now().toString(),
                "itens", List.of()
        );

        MvcResult result = mockMvc.perform(post("/notas-fiscais")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(operadorToken)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asLong();
    }

    private String chave(int valor) {
        String chaveSemDv = String.format("%043d", valor);
        return chaveSemDv
                + ValidadorChaveAcessoNfe.calcularDigitoVerificador(chaveSemDv);
    }

    private String formatarChave(String chave) {
        return chave.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
