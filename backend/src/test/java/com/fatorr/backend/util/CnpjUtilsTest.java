package com.fatorr.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CnpjUtilsTest {

    @Test
    @DisplayName("Deve limpar caracteres especiais mantendo padrão alfanumérico em caixa alta")
    void testUnmask() {
        assertEquals("12ABC345000101", CnpjUtils.unmask("12.ABC.345/0001-01"));
        assertEquals("98XYZ765000102", CnpjUtils.unmask("98.xyz.765/0001-02"));
        assertEquals("12345678000199", CnpjUtils.unmask("12.345.678/0001-99"));
        assertEquals("", CnpjUtils.unmask(null));
        assertEquals("", CnpjUtils.unmask(""));
    }

    @Test
    @DisplayName("Deve aplicar máscara no formato XX.XXX.XXX/XXXX-XX para CNPJ alfanumérico e numérico")
    void testMask() {
        assertEquals("12.ABC.345/0001-01", CnpjUtils.mask("12ABC345000101"));
        assertEquals("98.XYZ.765/0001-02", CnpjUtils.mask("98xyz765000102"));
        assertEquals("12.345.678/0001-99", CnpjUtils.mask("12345678000199"));
        assertEquals("", CnpjUtils.mask(null));
    }

    @Test
    @DisplayName("Deve validar CNPJ alfanumérico e numérico clássico com 14 caracteres")
    void testIsValid() {
        assertTrue(CnpjUtils.isValid("12ABC345000101"));
        assertTrue(CnpjUtils.isValid("12.ABC.345/0001-01"));
        assertTrue(CnpjUtils.isValid("98XYZ765000102"));
        assertTrue(CnpjUtils.isValid("12345678000199"));

        // Inválidos
        assertFalse(CnpjUtils.isValid("12ABC345")); // Menor que 14
        assertFalse(CnpjUtils.isValid("12ABC345000101999")); // Maior que 14
        assertFalse(CnpjUtils.isValid("12@BC345000101")); // Caractere inválido após unmask
        assertFalse(CnpjUtils.isValid(null));
        assertFalse(CnpjUtils.isValid(""));
    }
}
