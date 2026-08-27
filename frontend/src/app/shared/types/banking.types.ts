import { ContaReceberDTO } from './receivables.types';

export type TipoLancamento = 'CREDITO' | 'DEBITO';
export type StatusConciliacao = 'PENDENTE' | 'CONCILIADO';

export interface ExtratoLancamentoDTO {
  id: string;
  empresaId: string;
  banco: string;
  conta?: string;
  dataLancamento: string;
  descricao: string;
  valor: number;
  tipo: TipoLancamento;
  fitid: string; // ID único do lançamento no OFX para evitar duplicidade
  statusConciliacao: StatusConciliacao;
  createdAt?: string;
}

export interface ConciliarManualDTO {
  extratoLancamentoId: string;
  contaReceberId: string;
  observacao?: string;
}

export interface ConciliacaoBancariaDTO {
  id: string;
  extratoLancamentoId: string;
  contaReceberId?: string;
  usuarioId: string;
  dataConciliacao: string;
  observacao?: string;
  extratoLancamento?: ExtratoLancamentoDTO;
  contaReceber?: ContaReceberDTO;
}

export interface ImportExtratoResponseDTO {
  sucesso: boolean;
  banco: string;
  conta?: string;
  totalLancamentos: number;
  novosLancamentos: number;
  duplicadosIgnorados: number;
  lancamentos: ExtratoLancamentoDTO[];
}
