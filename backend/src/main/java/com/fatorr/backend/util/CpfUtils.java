package com.fatorr.backend.util;

/**
 * Utilitário universal para manipulação e validação de CPF.
 * Garante padronização sem pontuação e cálculo dos dígitos verificadores.
 */
public final class CpfUtils {

    private CpfUtils() {
        // Utility class
    }

    /**
     * Remove todos os caracteres não numéricos mantendo apenas os dígitos do CPF.
     */
    public static String unmask(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("\\D", "");
    }

    /**
     * Aplica a máscara padrão de CPF: XXX.XXX.XXX-XX
     */
    public static String mask(String cpf) {
        String clean = unmask(cpf);
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.length() > 11) {
            clean = clean.substring(0, 11);
        }

        StringBuilder sb = new StringBuilder();
        int len = clean.length();

        for (int i = 0; i < len; i++) {
            if (i == 3 || i == 6) {
                sb.append('.');
            } else if (i == 9) {
                sb.append('-');
            }
            sb.append(clean.charAt(i));
        }

        return sb.toString();
    }

    /**
     * Valida o tamanho, formato e os dois dígitos verificadores do CPF.
     */
    public static boolean isValid(String cpf) {
        String clean = unmask(cpf);
        if (clean.length() != 11) {
            return false;
        }

        // Rejeita sequências de dígitos idênticos (ex: 11111111111, 00000000000)
        if (clean.matches("(\\d)\\1{10}")) {
            return false;
        }

        // 1º dígito verificador
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (clean.charAt(i) - '0') * (10 - i);
        }
        int rev = 11 - (sum % 11);
        int digito1 = (rev == 10 || rev == 11) ? 0 : rev;
        if (digito1 != (clean.charAt(9) - '0')) {
            return false;
        }

        // 2º dígito verificador
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (clean.charAt(i) - '0') * (11 - i);
        }
        rev = 11 - (sum % 11);
        int digito2 = (rev == 10 || rev == 11) ? 0 : rev;
        return digito2 == (clean.charAt(10) - '0');
    }
}
