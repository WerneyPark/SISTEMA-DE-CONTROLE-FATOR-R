package com.fatorr.backend.util;

import java.util.regex.Pattern;

/**
 * Utilitário universal para manipulação e validação de CNPJ,
 * compatível com o padrão numérico tradicional e o novo padrão alfanumérico da RFB.
 */
public final class CnpjUtils {

    private static final Pattern ALPHANUMERIC_CNPJ_PATTERN = Pattern.compile("^[A-Z0-9]{14}$");

    private CnpjUtils() {
        // Utility class
    }

    /**
     * Remove todos os caracteres especiais mantendo apenas caracteres alfanuméricos em caixa alta.
     */
    public static String unmask(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    /**
     * Aplica a máscara padrão de CNPJ: XX.XXX.XXX/XXXX-XX
     * Suporta tanto dígitos numéricos quanto caracteres alfanuméricos.
     */
    public static String mask(String cnpj) {
        String clean = unmask(cnpj);
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.length() > 14) {
            clean = clean.substring(0, 14);
        }

        StringBuilder sb = new StringBuilder();
        int len = clean.length();

        for (int i = 0; i < len; i++) {
            if (i == 2 || i == 5) {
                sb.append('.');
            } else if (i == 8) {
                sb.append('/');
            } else if (i == 12) {
                sb.append('-');
            }
            sb.append(clean.charAt(i));
        }

        return sb.toString();
    }

    /**
     * Valida o tamanho e formato básico do CNPJ alfanumérico (14 caracteres alfanuméricos).
     */
    public static boolean isValid(String cnpj) {
        String clean = unmask(cnpj);
        if (clean.length() != 14) {
            return false;
        }
        return ALPHANUMERIC_CNPJ_PATTERN.matcher(clean).matches();
    }
}
