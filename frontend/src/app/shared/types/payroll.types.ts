export type OrigemFolha = 'MANUAL' | 'IMPORTACAO_ESOCIAL' | 'IMPORTACAO_CSV';

export interface FolhaDetalhadaDTO {
  id: string;
  empresaId: string;
  competencia: string; // Ex: "2026-08"
  valorSalarios: number;
  valorProLabore: number;
  valorDecimoTerceiro: number;
  valorInssPatronal: number;
  valorFgts: number;
  valorTotal: number;
  origem: OrigemFolha;
  xmlChaveRecibo?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateFolhaDetalhadaDTO {
  empresaId: string;
  competencia: string;
  valorSalarios: number;
  valorProLabore: number;
  valorDecimoTerceiro?: number;
  valorInssPatronal?: number;
  valorFgts?: number;
  origem?: OrigemFolha;
  xmlChaveRecibo?: string;
}

export interface ImportFolhaResponseDTO {
  sucesso: boolean;
  competencia: string;
  totalColaboradores: number;
  totalSocios: number;
  valorSalarios: number;
  valorProLabore: number;
  valorEncargos: number;
  valorTotalFolha: number;
  folhaGerada: FolhaDetalhadaDTO;
}

export interface ProLaboreIdealDTO {
  empresaId: string;
  competencia: string;
  faturamentoPrevisto: number;
  folhaAtualSemProLabore: number;
  folhaSemProLabore12?: number;
  proLaboreAtual12?: number;
  folhaSemProLaboreMes?: number;
  proLaboreAtualMes?: number;
  diferencaParaAnexo3?: number;
  proLaboreMinimoParaAnexo3: number;
  fatorRProjetado: number;
  anexoProjetado: 'Anexo III' | 'Anexo V';
  custoTributarioAdicional: {
    inssSocio: number;
    irpfSocio: number;
    custoTotal: number;
  };
  custoTributarioAnual?: number;
  aliquotaEfetivaAnexoV?: number;
  aliquotaEfetivaAnexoIII?: number;
  impostoAnexoVMensal?: number;
  impostoAnexoIIIMensal?: number;
  impostoAnexoVAnual?: number;
  impostoAnexoIIIAnual?: number;
  economiaSimplesNacional: number;
  economiaSimplesAnual?: number;
  saldoLiquidoVantagem: number;
  saldoLiquidoVantagemAnual?: number;
}
