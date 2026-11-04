export type StatusContaReceber = 'PENDENTE' | 'PAGO' | 'CANCELADO' | 'VENCIDO' | 'NF_CANCELADA';
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

export interface CreateContaReceberDTO {
  empresaId: string;
  clienteId: string;
  numeroDocumento: string;
  valorTotal: number;
  dataEmissao: string;
  dataVencimento: string;
  origem?: OrigemContaReceber;
  xmlChaveAcesso?: string;
}

export interface ImportXmlResponseDTO {
  sucesso: boolean;
  chaveNfe: string;
  numeroNota: string;
  emitenteCnpj: string;
  tomadorNome: string;
  tomadorCnpj: string;
  valorTotal: number;
  titulosGerados: ContaReceberDTO[];
}
