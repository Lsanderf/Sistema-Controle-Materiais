package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.notafiscal.ImportacaoXmlNfeResponse;
import com.Lucca.Projeto1.dto.notafiscal.ItemImportadoXmlNfeResponse;
import com.Lucca.Projeto1.exception.ArquivoMuitoGrandeException;
import com.Lucca.Projeto1.exception.XmlNfeInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImportacaoXmlNfeService {

    private static final String NAMESPACE_NFE =
            "http://www.portalfiscal.inf.br/nfe";
    private static final Set<String> VERSOES_SUPORTADAS = Set.of("4.00");
    private static final int LIMITE_QUANTIDADE_ITEM = 10000;

    private final ChaveAcessoNfeService chaveAcessoService;
    private final long tamanhoMaximoBytes;

    public ImportacaoXmlNfeService(
            ChaveAcessoNfeService chaveAcessoService,
            @Value("${app.notas-fiscais.xml.tamanho-maximo:2MB}")
            String tamanhoMaximo
    ) {
        this.chaveAcessoService = chaveAcessoService;
        this.tamanhoMaximoBytes = DataSize.parse(tamanhoMaximo).toBytes();
    }

    @Transactional(readOnly = true)
    public ImportacaoXmlNfeResponse importar(
            MultipartFile arquivo,
            String chaveAcessoInformada,
            Long notaFiscalId
    ) {
        Document documento = lerDocumento(lerConteudo(arquivo));
        Element nfe = localizarNfe(documento);
        Element infNfe = filhoObrigatorio(nfe, "infNFe", "NF-e");
        validarVersao(infNfe);

        String chaveAcesso = extrairChaveAcesso(infNfe);
        chaveAcessoService.validarCorrespondencia(
                chaveAcesso,
                chaveAcessoInformada
        );
        chaveAcessoService.validarDisponibilidade(
                chaveAcesso,
                notaFiscalId
        );

        Element ide = filhoObrigatorio(infNfe, "ide", "NF-e");
        validarModelo(ide);
        Element emitente = filhoObrigatorio(infNfe, "emit", "NF-e");

        List<ItemImportadoXmlNfeResponse> itens = extrairItens(infNfe);
        if (itens.isEmpty()) {
            throw xmlInvalido(
                    "O XML da NF-e não possui itens de produto para importar."
            );
        }

        String numero = textoObrigatorio(ide, "nNF", "identificação");
        String serie = textoObrigatorio(ide, "serie", "identificação");
        String fornecedor = textoObrigatorio(
                emitente,
                "xNome",
                "emitente"
        );
        String cnpjFornecedor = textoObrigatorio(
                emitente,
                "CNPJ",
                "emitente"
        );

        validarTamanho(numero, 50, "número da NF-e");
        validarTamanho(serie, 20, "série da NF-e");
        validarTamanho(fornecedor, 200, "nome do emitente");
        if (!cnpjFornecedor.matches("\\d{14}")) {
            throw xmlInvalido(
                    "O CNPJ do emitente no XML deve conter 14 dígitos."
            );
        }

        return new ImportacaoXmlNfeResponse(
                chaveAcesso,
                numero,
                serie,
                extrairDataEmissao(ide),
                cnpjFornecedor,
                fornecedor,
                List.copyOf(itens)
        );
    }

    private byte[] lerConteudo(MultipartFile arquivo) {
        if (arquivo == null) {
            throw xmlInvalido(
                    "Selecione um arquivo XML de NF-e para importar."
            );
        }
        if (arquivo.isEmpty()) {
            throw xmlInvalido(
                    "O arquivo XML da NF-e está vazio."
            );
        }
        if (arquivo.getSize() > tamanhoMaximoBytes) {
            throw arquivoMuitoGrande();
        }

        try {
            byte[] conteudo = arquivo.getBytes();
            if (conteudo.length == 0) {
                throw xmlInvalido(
                        "O arquivo XML da NF-e está vazio."
                );
            }
            if (conteudo.length > tamanhoMaximoBytes) {
                throw arquivoMuitoGrande();
            }
            return conteudo;
        } catch (IOException exception) {
            throw new XmlNfeInvalidoException(
                    "Não foi possível ler o arquivo XML da NF-e.",
                    exception
            );
        }
    }

    private Document lerDocumento(byte[] conteudo) {
        try {
            DocumentBuilderFactory factory = criarFactorySegura();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                throw new SAXException(
                        "Entidades externas não são permitidas"
                );
            });
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException exception)
                        throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception)
                        throws SAXException {
                    throw exception;
                }
            });

            try (ByteArrayInputStream input =
                         new ByteArrayInputStream(conteudo)) {
                return builder.parse(new InputSource(input));
            }
        } catch (ParserConfigurationException exception) {
            throw new IllegalStateException(
                    "Não foi possível configurar o leitor seguro de XML",
                    exception
            );
        } catch (SAXException | IOException exception) {
            throw new XmlNfeInvalidoException(
                    "O arquivo não contém um XML de NF-e válido ou está malformado.",
                    exception
            );
        }
    }

    private DocumentBuilderFactory criarFactorySegura()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );
        factory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        );
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private Element localizarNfe(Document documento) {
        Element raiz = documento.getDocumentElement();
        if (raiz == null || !NAMESPACE_NFE.equals(raiz.getNamespaceURI())) {
            throw xmlInvalido(
                    "O arquivo informado não possui a estrutura oficial de uma NF-e."
            );
        }

        return switch (raiz.getLocalName()) {
            case "NFe" -> raiz;
            case "nfeProc" -> filhoObrigatorio(
                    raiz,
                    "NFe",
                    "processo da NF-e"
            );
            default -> throw xmlInvalido(
                    "O arquivo informado não representa uma NF-e."
            );
        };
    }

    private void validarVersao(Element infNfe) {
        String versao = infNfe.getAttribute("versao").trim();
        if (!VERSOES_SUPORTADAS.contains(versao)) {
            throw xmlInvalido(
                    "A versão " + (versao.isBlank() ? "não informada" : versao)
                            + " do XML da NF-e não é suportada."
            );
        }
    }

    private String extrairChaveAcesso(Element infNfe) {
        String id = infNfe.getAttribute("Id").trim();
        if (!id.startsWith("NFe") || id.length() != 47) {
            throw xmlInvalido(
                    "O XML da NF-e não possui uma chave de acesso válida no atributo Id."
            );
        }
        return chaveAcessoService.normalizarEValidar(id.substring(3));
    }

    private void validarModelo(Element ide) {
        String modelo = textoObrigatorio(ide, "mod", "identificação");
        if (!"55".equals(modelo)) {
            throw xmlInvalido(
                    "O XML informado não representa uma NF-e modelo 55."
            );
        }
    }

    private LocalDate extrairDataEmissao(Element ide) {
        String dataHora = textoOpcional(ide, "dhEmi");
        String data = textoOpcional(ide, "dEmi");

        try {
            if (dataHora != null) {
                return OffsetDateTime.parse(dataHora).toLocalDate();
            }
            if (data != null) {
                return LocalDate.parse(data);
            }
        } catch (DateTimeParseException exception) {
            throw new XmlNfeInvalidoException(
                    "A data de emissão presente no XML da NF-e é inválida.",
                    exception
            );
        }

        throw xmlInvalido(
                "O XML da NF-e não informa a data de emissão."
        );
    }

    private List<ItemImportadoXmlNfeResponse> extrairItens(Element infNfe) {
        List<Element> detalhes = filhosDiretos(infNfe, "det");
        List<ItemImportadoXmlNfeResponse> itens = new ArrayList<>();
        Set<Integer> numerosEncontrados = new HashSet<>();

        for (Element detalhe : detalhes) {
            int numeroItem = numeroItem(detalhe);
            if (!numerosEncontrados.add(numeroItem)) {
                throw xmlInvalido(
                        "O XML possui mais de um item com o número "
                                + numeroItem + "."
                );
            }

            Element produto = filhoObrigatorio(
                    detalhe,
                    "prod",
                    "item " + numeroItem
            );
            BigDecimal quantidade = quantidadeCompativel(
                    decimalObrigatorio(
                            produto,
                            "qCom",
                            "item " + numeroItem
                    ),
                    numeroItem
            );
            BigDecimal valorUnitario = valorMonetarioCompativel(
                    decimalObrigatorio(
                            produto,
                            "vUnCom",
                            "item " + numeroItem
                    ),
                    "valor unitário comercial",
                    numeroItem
            );
            BigDecimal valorTotal = decimalOpcional(produto, "vProd");
            if (valorTotal != null) {
                valorTotal = valorMonetarioCompativel(
                        valorTotal,
                        "valor total",
                        numeroItem
                );
            }

            itens.add(new ItemImportadoXmlNfeResponse(
                    numeroItem,
                    textoObrigatorio(
                            produto,
                            "cProd",
                            "item " + numeroItem
                    ),
                    textoObrigatorio(
                            produto,
                            "xProd",
                            "item " + numeroItem
                    ),
                    quantidade,
                    textoObrigatorio(
                            produto,
                            "uCom",
                            "item " + numeroItem
                    ),
                    valorUnitario,
                    valorTotal,
                    textoOpcional(produto, "cEAN")
            ));
        }
        return itens;
    }

    private int numeroItem(Element detalhe) {
        String valor = detalhe.getAttribute("nItem").trim();
        try {
            int numero = Integer.parseInt(valor);
            if (numero <= 0) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException exception) {
            throw new XmlNfeInvalidoException(
                    "Um item da NF-e possui o atributo nItem inválido.",
                    exception
            );
        }
    }

    private BigDecimal quantidadeCompativel(
            BigDecimal quantidade,
            int numeroItem
    ) {
        if (quantidade.signum() <= 0) {
            throw xmlInvalido(
                    "A quantidade comercial do item " + numeroItem
                            + " deve ser maior que zero."
            );
        }

        BigDecimal normalizada = normalizarDecimal(quantidade);
        if (normalizada.scale() > 0) {
            throw xmlInvalido(
                    "A quantidade comercial do item " + numeroItem
                            + " possui casas decimais e não é compatível com o modelo atual, "
                            + "que aceita apenas quantidades inteiras."
            );
        }

        if (normalizada.compareTo(
                BigDecimal.valueOf(LIMITE_QUANTIDADE_ITEM)
        ) > 0) {
            throw xmlInvalido(
                    "A quantidade comercial do item " + numeroItem
                            + " excede o limite atual de 10.000 unidades."
            );
        }
        int quantidadeInteira = normalizada.intValueExact();
        return BigDecimal.valueOf(quantidadeInteira);
    }

    private BigDecimal valorMonetarioCompativel(
            BigDecimal valor,
            String campo,
            int numeroItem
    ) {
        if (valor.signum() < 0) {
            throw xmlInvalido(
                    "O " + campo + " do item " + numeroItem
                            + " não pode ser negativo."
            );
        }

        BigDecimal normalizado = normalizarDecimal(valor);
        if (normalizado.scale() > 2 || normalizado.precision() > 19) {
            throw xmlInvalido(
                    "O " + campo + " do item " + numeroItem
                            + " não é compatível com o limite atual de 2 casas decimais."
            );
        }
        return normalizado;
    }

    private BigDecimal decimalObrigatorio(
            Element pai,
            String nome,
            String contexto
    ) {
        String valor = textoObrigatorio(pai, nome, contexto);
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException exception) {
            throw new XmlNfeInvalidoException(
                    "O campo " + nome + " do " + contexto
                            + " possui um valor decimal inválido.",
                    exception
            );
        }
    }

    private BigDecimal decimalOpcional(Element pai, String nome) {
        String valor = textoOpcional(pai, nome);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException exception) {
            throw new XmlNfeInvalidoException(
                    "O campo " + nome + " possui um valor decimal inválido.",
                    exception
            );
        }
    }

    private BigDecimal normalizarDecimal(BigDecimal valor) {
        BigDecimal normalizado = valor.stripTrailingZeros();
        return normalizado.scale() < 0
                ? normalizado.setScale(0)
                : normalizado;
    }

    private String textoObrigatorio(
            Element pai,
            String nome,
            String contexto
    ) {
        String valor = textoOpcional(pai, nome);
        if (valor == null) {
            throw xmlInvalido(
                    "O campo " + nome + " é obrigatório no " + contexto + "."
            );
        }
        return valor;
    }

    private String textoOpcional(Element pai, String nome) {
        Element filho = filhoDireto(pai, nome);
        if (filho == null) {
            return null;
        }
        String valor = filho.getTextContent().trim();
        return valor.isBlank() ? null : valor;
    }

    private Element filhoObrigatorio(
            Element pai,
            String nome,
            String contexto
    ) {
        Element filho = filhoDireto(pai, nome);
        if (filho == null) {
            throw xmlInvalido(
                    "O elemento " + nome + " é obrigatório no " + contexto + "."
            );
        }
        return filho;
    }

    private Element filhoDireto(Element pai, String nome) {
        for (Element elemento : filhosDiretos(pai, nome)) {
            return elemento;
        }
        return null;
    }

    private List<Element> filhosDiretos(Element pai, String nome) {
        List<Element> encontrados = new ArrayList<>();
        NodeList filhos = pai.getChildNodes();
        for (int indice = 0; indice < filhos.getLength(); indice++) {
            Node node = filhos.item(indice);
            if (node instanceof Element elemento
                    && NAMESPACE_NFE.equals(elemento.getNamespaceURI())
                    && nome.equals(elemento.getLocalName())) {
                encontrados.add(elemento);
            }
        }
        return encontrados;
    }

    private void validarTamanho(String valor, int maximo, String campo) {
        if (valor.length() > maximo) {
            throw xmlInvalido(
                    "O " + campo + " excede o tamanho aceito pelo sistema."
            );
        }
    }

    private XmlNfeInvalidoException xmlInvalido(String mensagem) {
        return new XmlNfeInvalidoException(mensagem);
    }

    private ArquivoMuitoGrandeException arquivoMuitoGrande() {
        return new ArquivoMuitoGrandeException(
                "O arquivo XML da NF-e excede o tamanho máximo permitido."
        );
    }
}
