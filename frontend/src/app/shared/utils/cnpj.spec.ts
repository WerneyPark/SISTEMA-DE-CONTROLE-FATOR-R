import { describe, it, expect } from 'vitest';
import { unmaskCnpj, maskCnpj, isValidCnpj } from './cnpj';

describe('Universal Alphanumeric CNPJ Utility (RFB)', () => {
  it('should unmask alphanumeric CNPJ to uppercase without special characters', () => {
    expect(unmaskCnpj('12.ABC.345/0001-01')).toBe('12ABC345000101');
    expect(unmaskCnpj('98.xyz.765/0001-02')).toBe('98XYZ765000102');
    expect(unmaskCnpj('12.345.678/0001-99')).toBe('12345678000199');
    expect(unmaskCnpj('')).toBe('');
  });

  it('should mask alphanumeric CNPJ in XX.XXX.XXX/XXXX-XX format', () => {
    expect(maskCnpj('12ABC345000101')).toBe('12.ABC.345/0001-01');
    expect(maskCnpj('98xyz765000102')).toBe('98.XYZ.765/0001-02');
    expect(maskCnpj('12345678000199')).toBe('12.345.678/0001-99');
    expect(maskCnpj('')).toBe('');
  });

  it('should validate alphanumeric and traditional CNPJ of length 14', () => {
    expect(isValidCnpj('12ABC345000101')).toBe(true);
    expect(isValidCnpj('12.ABC.345/0001-01')).toBe(true);
    expect(isValidCnpj('98XYZ765000102')).toBe(true);
    expect(isValidCnpj('12345678000199')).toBe(true);

    expect(isValidCnpj('12ABC345')).toBe(false);
    expect(isValidCnpj('12ABC345000101999')).toBe(false);
    expect(isValidCnpj('12@BC345000101')).toBe(false);
    expect(isValidCnpj('')).toBe(false);
  });
});
