export type FormatoRelatorio = 'PDF' | 'EXCEL';
export type TipoLaudo = 'LAUDO_ENQUADRAMENTO_FATOR_R' | 'MEMORIA_CALCULO_12M' | 'ESTUDO_COMPARATIVO_REGIMES' | 'DOSSIE_COMPLETO';

export interface ParametrosEmissaoLaudoDTO {
  empresaId: string;
  competencia: string; // Ex: "2026-08"
  tipoLaudo: TipoLaudo;
  formato: FormatoRelatorio;
  incluirProjecaoCenarios: boolean;
  incluirAssinaturaContador: boolean;
  observacaoPersonalizada?: string;
}

export interface LaudoTributarioDTO {
  id: string;
  empresaId: string;
  usuarioId: string;
  usuarioNome?: string;
  competencia: string;
  tipoLaudo: TipoLaudo;
  hashAutenticidade: string;
  caminhoArquivo?: string;
  urlDownload?: string;
  createdAt: string;
}

export interface ValidacaoLaudoResponseDTO {
  valido: boolean;
  hashAutenticidade: string;
  dataEmissao: string;
  empresaNome: string;
  empresaCnpj: string;
  contadorResponsavel: string;
  crcContador: string;
  competencia: string;
  anexoEnquadrado: string;
  percentualFatorR: number;
}
