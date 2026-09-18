import { 
  FolhaDetalhadaDTO, 
  CreateFolhaDetalhadaDTO, 
  ImportFolhaResponseDTO, 
  ProLaboreIdealDTO 
} from '../../../shared/types/payroll.types';
import { ApiResponse, PaginatedResult, PaginationParams } from '../../../shared/types/common.types';
import { PayrollService } from '../services/payroll.service';

export class PayrollController {
  constructor(private readonly payrollService: PayrollService = new PayrollService()) {}

  async listFolhas(empresaId: string, params?: PaginationParams): Promise<ApiResponse<PaginatedResult<FolhaDetalhadaDTO>>> {
    return this.payrollService.listFolhas(empresaId, params);
  }

  async getFolhaByCompetencia(empresaId: string, competencia: string): Promise<ApiResponse<FolhaDetalhadaDTO | null>> {
    return this.payrollService.getFolhaByCompetencia(empresaId, competencia);
  }

  async saveFolha(dto: CreateFolhaDetalhadaDTO): Promise<ApiResponse<FolhaDetalhadaDTO>> {
    return this.payrollService.saveFolha(dto);
  }

  async importEsocialXml(empresaId: string, xmlContent: string): Promise<ApiResponse<ImportFolhaResponseDTO>> {
    return this.payrollService.importEsocialXml(empresaId, xmlContent);
  }

  async importFolhaCsv(empresaId: string, csvContent: string): Promise<ApiResponse<ImportFolhaResponseDTO>> {
    return this.payrollService.importFolhaCsv(empresaId, csvContent);
  }

  async calcularProLaboreIdeal(empresaId: string, competencia: string, faturamentoPrevisto: number): Promise<ApiResponse<ProLaboreIdealDTO>> {
    return this.payrollService.calcularProLaboreIdeal(empresaId, competencia, faturamentoPrevisto);
  }
}
