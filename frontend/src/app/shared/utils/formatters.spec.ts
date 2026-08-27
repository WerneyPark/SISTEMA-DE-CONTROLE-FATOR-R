import { describe, it, expect } from 'vitest';
import { formatCurrency, unmaskCpf, maskCpf, isValidCpf } from './formatters';
import { formatDateToBR, parseCompetencia } from './date';

describe('Shared Formatters & Date Utilities', () => {
  it('should format currency correctly in BRL', () => {
    const formatted = formatCurrency(15000.50);
    expect(formatted).toContain('15.000,50');
  });

  it('should unmask and mask CPF', () => {
    expect(unmaskCpf('111.222.333-44')).toBe('11122233344');
    expect(maskCpf('11122233344')).toBe('111.222.333-44');
  });

  it('should validate CPF', () => {
    // CPF de teste com dígitos verificadores válidos
    expect(isValidCpf('52998224725')).toBe(true);
    expect(isValidCpf('11111111111')).toBe(false);
    expect(isValidCpf('123')).toBe(false);
  });

  it('should format date and parse competencia', () => {
    expect(formatDateToBR('2026-08-27')).toBe('27/08/2026');
    expect(parseCompetencia('2026-08')).toEqual({ ano: 2026, mes: 8 });
  });
});
