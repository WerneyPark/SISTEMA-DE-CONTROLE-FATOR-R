package com.fatorr.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfUtilsTest {

    @Test
    @DisplayName("Deve remover pontuação e caracteres não numéricos do CPF")
    void testUnmask() {
        assertEquals("11122233344", CpfUtils.unmask("111.222.333-44"));
        assertEquals("12345678909", CpfUtils.unmask(" 123.456.789-09 "));
        assertEquals("11122233344", CpfUtils.unmask("11122233344"));
        assertEquals("", CpfUtils.unmask(null));
        assertEquals("", CpfUtils.unmask(""));
    }

    @Test
    @DisplayName("Deve aplicar máscara no formato XXX.XXX.XXX-XX")
    void testMask() {
        assertEquals("111.222.333-44", CpfUtils.mask("11122233344"));
        assertEquals("111.222.333-44", CpfUtils.mask("111.222.333-44"));
        assertEquals("", CpfUtils.mask(null));
        assertEquals("", CpfUtils.mask(""));
    }

    @Test
    @DisplayName("Deve validar CPF válido com dígitos verificadores corretos")
    void testIsValidValido() {
        // CPFs válidos conhecidos para testes
        assertTrue(CpfUtils.isValid("52998224725"));
        assertTrue(CpfUtils.isValid("529.982.247-25"));
        assertTrue(CpfUtils.isValid("12345678909"));
    }

    @Test
    @DisplayName("Deve invalidar CPFs com dígitos repetidos, tamanhos incorretos ou dígitos inválidos")
    void testIsValidInvalido() {
        assertFalse(CpfUtils.isValid("11111111111"));
        assertFalse(CpfUtils.isValid("00000000000"));
        assertFalse(CpfUtils.isValid("123"));
        assertFalse(CpfUtils.isValid("11122233344")); // Dígitos verificadores incorretos
        assertFalse(CpfUtils.isValid(null));
        assertFalse(CpfUtils.isValid(""));
    }
}
