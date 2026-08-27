import { 
  ExtratoLancamentoDTO, 
  ConciliarManualDTO, 
  ConciliacaoBancariaDTO, 
  ImportExtratoResponseDTO 
} from '../../../shared/types/banking.types';
import { ApiResponse, PaginatedResult, PaginationParams } from '../../../shared/types/common.types';
import { BankingService } from '../services/banking.service';

export class BankingController {
  constructor(private readonly bankingService: BankingService = new BankingService()) {}

  async listLancamentos(empresaId: string, params?: PaginationParams): Promise<ApiResponse<PaginatedResult<ExtratoLancamentoDTO>>> {
    return this.bankingService.listLancamentos(empresaId, params);
  }

  async importExtrato(empresaId: string, fileContent: string, formato: 'OFX' | 'CSV'): Promise<ApiResponse<ImportExtratoResponseDTO>> {
    return this.bankingService.importExtrato(empresaId, fileContent, formato);
  }

  async conciliarManual(dto: ConciliarManualDTO, usuarioId: string): Promise<ApiResponse<ConciliacaoBancariaDTO>> {
    return this.bankingService.conciliarManual(dto, usuarioId);
  }

  async listConciliacoes(empresaId: string): Promise<ApiResponse<ConciliacaoBancariaDTO[]>> {
    return this.bankingService.listConciliacoes(empresaId);
  }
}
