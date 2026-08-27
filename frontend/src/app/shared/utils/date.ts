export function formatDateToISO(date: Date | string): string {
  if (!date) return '';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d.getTime())) return '';
  return d.toISOString().split('T')[0];
}

export function formatDateToBR(dateString: string): string {
  if (!dateString) return '';
  const clean = dateString.split('T')[0];
  const parts = clean.split('-');
  if (parts.length !== 3) return dateString;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

export function parseCompetencia(competencia: string): { ano: number; mes: number } | null {
  if (!competencia) return null;
  const match = competencia.match(/^(\d{4})-(\d{2})$/);
  if (!match) return null;
  return {
    ano: parseInt(match[1], 10),
    mes: parseInt(match[2], 10)
  };
}

export function formatCompetenciaBR(competencia: string): string {
  const parsed = parseCompetencia(competencia);
  if (!parsed) return competencia;
  const meses = [
    'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'
  ];
  return `${meses[parsed.mes - 1]}/${parsed.ano}`;
}
