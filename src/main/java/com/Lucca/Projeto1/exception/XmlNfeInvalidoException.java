package com.Lucca.Projeto1.exception;

public class XmlNfeInvalidoException extends RuntimeException {

    public XmlNfeInvalidoException(String mensagem) {
        super(mensagem);
    }

    public XmlNfeInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
