/**
 * Utilitário universal para CNPJ (suporta numérico clássico e o novo padrão alfanumérico da RFB)
 */

// Limpa caracteres especiais mantendo apenas caracteres alfanuméricos em caixa alta
export function unmaskCnpj(value: string): string {
  if (!value) return '';
  return value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
}

// Aplica a máscara padrão XX.XXX.XXX/XXXX-XX (para letras e números)
export function maskCnpj(value: string): string {
  const clean = unmaskCnpj(value).slice(0, 14);
  if (!clean) return '';
  
  return clean
    .replace(/^([a-zA-Z0-9]{2})([a-zA-Z0-9])/, '$1.$2')
    .replace(/^([a-zA-Z0-9]{2})\.([a-zA-Z0-9]{3})([a-zA-Z0-9])/, '$1.$2.$3')
    .replace(/\.([a-zA-Z0-9]{3})([a-zA-Z0-9])/, '.$1/$2')
    .replace(/\/([a-zA-Z0-9]{4})([a-zA-Z0-9])/, '/$1-$2');
}

// Valida tamanho e formato básico do CNPJ alfanumérico
export function isValidCnpj(value: string): boolean {
  const clean = unmaskCnpj(value);
  if (clean.length !== 14) return false;
  
  // Aceita apenas letras A-Z e números 0-9
  const alphanumericRegex = /^[A-Z0-9]{14}$/;
  return alphanumericRegex.test(clean);
}
