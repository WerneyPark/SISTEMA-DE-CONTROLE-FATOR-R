export type StatusContaReceber = 'PENDENTE' | 'PAGO' | 'CANCELADO' | 'VENCIDO';
export type OrigemContaReceber = 'MANUAL' | 'IMPORTACAO_XML';
export type TipoClienteFornecedor = 'CLIENTE' | 'FORNECEDOR' | 'AMBOS';

export interface ClienteFornecedorDTO {
  id: string;
  empresaId: string;
  nome: string;
  cnpjCpf: string; // Alfanumérico
  tipo: TipoClienteFornecedor;
  createdAt?: string;
}

export interface ContaReceberDTO {
  id: string;
  empresaId: string;
  clienteId: string;
  clienteNome?: string;
  clienteCnpj?: string;
  numeroDocumento: string;
  valorTotal: number;
  dataEmissao: string;
  dataVencimento: string;
  dataPagamento?: string;
  status: StatusContaReceber;
  origem: OrigemContaReceber;
  xmlChaveAcesso?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface InclusaoManualContaReceberDTO {
  empresaId: string;
  clienteId: string;
  numeroDocumento: string;
  valorTotal: number;
  dataEmissao: string;
  dataVencimento: string;
}

export interface FiltrosContasReceberDTO {
  empresaId: string;
  status?: StatusContaReceber;
  clienteId?: string;
  dataVencimentoInicio?: string;
  dataVencimentoFim?: string;
  page?: number;
  limit?: number;
  sortBy?: 'dataVencimento' | 'dataEmissao' | 'valorTotal' | 'createdAt';
  sortOrder?: 'asc' | 'desc';
}

export interface RespostaPaginadaContasReceberDTO {
  items: ContaReceberDTO[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface PreviewXmlDTO {
  empresaId: string;
  tipoDocumento: 'NFE' | 'NFSE';
  chaveAcesso: string;
  numeroNota: string;
  dataEmissao: string;
  emitente: {
    nome: string;
    cnpj: string;
  };
  tomador: {
    nome: string;
    cnpj: string;
    clienteExistente: boolean;
    clienteId?: string;
  };
  valorTotal: number;
  parcelas: Array<{
    numero: string;
    valor: number;
    dataVencimento: string;
  }>;
}

export interface ResultadoImportacaoDTO {
  sucesso: boolean;
  mensagem: string;
  preview: PreviewXmlDTO;
  clienteCriado: boolean;
  clienteId: string;
  titulosGerados: ContaReceberDTO[];
  duplicadosIgnorados: number;
}

/** Alias mantido para compatibilidade com o contrato anterior. */
export type CreateContaReceberDTO = InclusaoManualContaReceberDTO;

/** Alias mantido para compatibilidade com o contrato anterior. */
export type ImportXmlResponseDTO = ResultadoImportacaoDTO;

export interface PreviewXmlRequestDTO {
  empresaId: string;
  xmlContent: string;
}

export interface BaixarContaReceberDTO {
  contaReceberId: string;
  dataPagamento: string;
}