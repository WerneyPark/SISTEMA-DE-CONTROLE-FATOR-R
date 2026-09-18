import { 
  ParametrosEmissaoLaudoDTO, 
  LaudoTributarioDTO, 
  ValidacaoLaudoResponseDTO 
} from '../../../shared/types/reports.types';
import { ApiResponse, PaginatedResult, PaginationParams } from '../../../shared/types/common.types';
import { ReportsService } from '../services/reports.service';

export class ReportsController {
  constructor(private readonly reportsService: ReportsService = new ReportsService()) {}

  async emitirLaudo(dto: ParametrosEmissaoLaudoDTO, usuarioId: string): Promise<ApiResponse<LaudoTributarioDTO>> {
    return this.reportsService.emitirLaudo(dto, usuarioId);
  }

  async listLaudos(empresaId: string, params?: PaginationParams): Promise<ApiResponse<PaginatedResult<LaudoTributarioDTO>>> {
    return this.reportsService.listLaudos(empresaId, params);
  }

  async validarAutenticidade(hash: string): Promise<ApiResponse<ValidacaoLaudoResponseDTO>> {
    return this.reportsService.validarAutenticidade(hash);
  }

  async exportarExcelAnalitico(empresaId: string, competencia: string): Promise<ApiResponse<{ urlDownload: string }>> {
    return this.reportsService.exportarExcelAnalitico(empresaId, competencia);
  }
}
