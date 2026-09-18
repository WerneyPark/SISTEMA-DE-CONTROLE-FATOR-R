import { describe, it, expect } from 'vitest';
import { FolhaDetalhadaDTO, ProLaboreIdealDTO } from '../types/payroll.types';
import { LaudoTributarioDTO, ValidacaoLaudoResponseDTO } from '../types/reports.types';

describe('Trilha 4 & Trilha 5 Contracts', () => {
  it('should instantiate FolhaDetalhadaDTO with valid structure', () => {
    const folha: FolhaDetalhadaDTO = {
      id: 'folha-1',
      empresaId: 'emp-1',
      competencia: '2026-08',
      valorSalarios: 10000,
      valorProLabore: 4000,
      valorDecimoTerceiro: 0,
      valorInssPatronal: 2500,
      valorFgts: 800,
      valorTotal: 17300,
      origem: 'IMPORTACAO_ESOCIAL'
    };

    expect(folha.valorTotal).toBe(17300);
    expect(folha.origem).toBe('IMPORTACAO_ESOCIAL');
  });

  it('should calculate pro-labore ideal simulation correctly', () => {
    const simulacao: ProLaboreIdealDTO = {
      empresaId: 'emp-1',
      competencia: '2026-08',
      faturamentoPrevisto: 100000,
      folhaAtualSemProLabore: 10000,
      proLaboreMinimoParaAnexo3: 18000,
      fatorRProjetado: 28.00,
      anexoProjetado: 'Anexo III',
      custoTributarioAdicional: {
        inssSocio: 1980,
        irpfSocio: 2330,
        custoTotal: 4310
      },
      economiaSimplesNacional: 9500,
      saldoLiquidoVantagem: 5190
    };

    expect(simulacao.fatorRProjetado).toBe(28.00);
    expect(simulacao.saldoLiquidoVantagem).toBeGreaterThan(0);
  });

  it('should instantiate LaudoTributarioDTO with hash and metadata', () => {
    const laudo: LaudoTributarioDTO = {
      id: 'laudo-1',
      empresaId: 'emp-1',
      usuarioId: 'usr-1',
      competencia: '2026-08',
      tipoLaudo: 'LAUDO_ENQUADRAMENTO_FATOR_R',
      hashAutenticidade: 'SHA256-TEST1234567890',
      createdAt: '2026-08-27T15:00:00Z'
    };

    expect(laudo.tipoLaudo).toBe('LAUDO_ENQUADRAMENTO_FATOR_R');
    expect(laudo.hashAutenticidade).toContain('SHA256');
  });
});
