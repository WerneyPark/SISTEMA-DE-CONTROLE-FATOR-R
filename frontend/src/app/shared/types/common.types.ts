export interface OrganizacaoContabilDTO {
  id: string;
  nome: string;
  cnpj: string; // Alfanumérico
  crc: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmpresaDTO {
  id: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string; // Alfanumérico
  organizacaoContabilId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmpresaUsuarioDTO {
  id: string;
  empresaId: string;
  usuarioId: string;
  funcao: string;
  statusVinculo: 'PENDENTE' | 'APROVADO' | 'REJEITADO';
  aprovadoPor?: string;
  aprovadoEm?: string;
  createdAt?: string;
  empresa?: EmpresaDTO;
  usuario?: {
    id: string;
    nome: string;
    email: string;
    cpf: string;
  };
}

export interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
  timestamp?: string;
}

export interface PaginationParams {
  page?: number;
  limit?: number;
  search?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PaginatedResult<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}
