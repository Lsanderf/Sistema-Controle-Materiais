package com.Lucca.Projeto1.validation;

public final class ValidadorChaveAcessoNfe {

    public static final int TAMANHO_CHAVE = 44;
    public static final int TAMANHO_SEM_DV = TAMANHO_CHAVE - 1;

    private ValidadorChaveAcessoNfe() {
    }

    public static boolean isValida(String chaveAcesso) {
        if (chaveAcesso == null
                || chaveAcesso.length() != TAMANHO_CHAVE
                || !contemSomenteDigitos(chaveAcesso)) {
            return false;
        }

        String chaveSemDv = chaveAcesso.substring(0, TAMANHO_SEM_DV);
        int dvInformado = Character.digit(
                chaveAcesso.charAt(TAMANHO_SEM_DV),
                10
        );

        return calcularDigitoVerificador(chaveSemDv) == dvInformado;
    }

    public static int calcularDigitoVerificador(String chaveSemDv) {
        if (chaveSemDv == null
                || chaveSemDv.length() != TAMANHO_SEM_DV
                || !contemSomenteDigitos(chaveSemDv)) {
            throw new IllegalArgumentException(
                    "A chave sem DV deve conter exatamente 43 dígitos"
            );
        }

        int soma = 0;
        int peso = 2;

        for (int indice = chaveSemDv.length() - 1; indice >= 0; indice--) {
            soma += Character.digit(chaveSemDv.charAt(indice), 10) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }

        int dv = 11 - (soma % 11);
        return dv == 10 || dv == 11 ? 0 : dv;
    }

    private static boolean contemSomenteDigitos(String valor) {
        return valor.chars().allMatch(caractere ->
                caractere >= '0' && caractere <= '9'
        );
    }
}
