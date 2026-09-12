package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import com.Lucca.Projeto1.validation.ValidadorChaveAcessoNfe;
import org.springframework.stereotype.Service;

@Service
public class ChaveAcessoNfeService {

    private final NotaFiscalEntradaRepository notaFiscalRepository;

    public ChaveAcessoNfeService(
            NotaFiscalEntradaRepository notaFiscalRepository
    ) {
        this.notaFiscalRepository = notaFiscalRepository;
    }

    public String normalizarEValidarDisponibilidade(
            String chaveAcesso,
            Long notaFiscalId
    ) {
        String normalizada = normalizarEValidar(chaveAcesso);
        validarDisponibilidade(normalizada, notaFiscalId);
        return normalizada;
    }

    public String normalizarEValidar(String chaveAcesso) {
        if (chaveAcesso == null
                || !chaveAcesso.matches("[0-9.\\-/\\s]+")) {
            throw new RegraNegocioException(
                    "A chave de acesso deve conter apenas números e formatação"
            );
        }

        String normalizada = chaveAcesso.replaceAll("\\D", "");
        if (normalizada.length() != 44) {
            throw new RegraNegocioException(
                    "A chave de acesso deve conter exatamente 44 dígitos"
            );
        }
        if (!ValidadorChaveAcessoNfe.isValida(normalizada)) {
            throw new RegraNegocioException(
                    "Chave de acesso da NF-e inválida. Verifique os números informados."
            );
        }
        return normalizada;
    }

    public void validarCorrespondencia(
            String chaveAcessoXml,
            String chaveAcessoInformada
    ) {
        if (chaveAcessoInformada == null
                || chaveAcessoInformada.isBlank()) {
            return;
        }

        String informadaNormalizada = normalizarEValidar(
                chaveAcessoInformada
        );
        if (!chaveAcessoXml.equals(informadaNormalizada)) {
            throw new RegraNegocioException(
                    "A chave da NF-e informada é diferente da chave presente no XML."
            );
        }
    }

    public void validarDisponibilidade(
            String chaveAcesso,
            Long notaFiscalId
    ) {
        boolean duplicada = notaFiscalId == null
                ? notaFiscalRepository.existsByChaveAcesso(chaveAcesso)
                : notaFiscalRepository.existsByChaveAcessoAndIdNot(
                        chaveAcesso,
                        notaFiscalId
                );

        if (duplicada) {
            throw new RegraNegocioException(
                    "Já existe uma nota fiscal com essa chave de acesso"
            );
        }
    }
}
