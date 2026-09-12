package com.Lucca.Projeto1;

import com.Lucca.Projeto1.validation.ValidadorChaveAcessoNfe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorChaveAcessoNfeTests {

    private static final String CHAVE_VALIDA_MANUAL =
            "52060433009911002506550120000007800267301615";

    @Test
    void aceitaChaveComDigitoVerificadorCorreto() {
        assertTrue(ValidadorChaveAcessoNfe.isValida(CHAVE_VALIDA_MANUAL));
        assertEquals(
                5,
                ValidadorChaveAcessoNfe.calcularDigitoVerificador(
                        CHAVE_VALIDA_MANUAL.substring(0, 43)
                )
        );
    }

    @Test
    void rejeitaDigitoVerificadorIncorreto() {
        String chaveInvalida = CHAVE_VALIDA_MANUAL.substring(0, 43) + "4";
        assertFalse(ValidadorChaveAcessoNfe.isValida(chaveInvalida));
    }

    @Test
    void rejeitaAlteracaoDeUmUnicoDigito() {
        String chaveAlterada = "6" + CHAVE_VALIDA_MANUAL.substring(1);
        assertFalse(ValidadorChaveAcessoNfe.isValida(chaveAlterada));
    }

    @Test
    void calculaDvZeroParaRestosZeroEUm() {
        String restoZero = "0".repeat(43);
        String restoUm = "0".repeat(42) + "6";

        assertEquals(
                0,
                ValidadorChaveAcessoNfe.calcularDigitoVerificador(restoZero)
        );
        assertEquals(
                0,
                ValidadorChaveAcessoNfe.calcularDigitoVerificador(restoUm)
        );
        assertTrue(ValidadorChaveAcessoNfe.isValida(restoZero + "0"));
        assertTrue(ValidadorChaveAcessoNfe.isValida(restoUm + "0"));
    }

    @Test
    void rejeitaChavesComTamanhoIncorreto() {
        assertFalse(ValidadorChaveAcessoNfe.isValida("0".repeat(43)));
        assertFalse(ValidadorChaveAcessoNfe.isValida("0".repeat(45)));
        assertThrows(
                IllegalArgumentException.class,
                () -> ValidadorChaveAcessoNfe.calcularDigitoVerificador(
                        "0".repeat(42)
                )
        );
    }
}
