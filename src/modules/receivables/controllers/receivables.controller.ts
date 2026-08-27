import { 
  ContaReceberDTO, 
  CreateContaReceberDTO, 
  ImportXmlResponseDTO, 
  ClienteFornecedorDTO 
} from '../../../shared/types/receivables.types';
import { ApiResponse, PaginatedResult, PaginationParams } from '../../../shared/types/common.types';
import { ReceivablesService } from '../services/receivables.service';

export class ReceivablesController {
  constructor(private readonly receivablesService: ReceivablesService = new ReceivablesService()) {}

  async listContasReceber(empresaId: string, params?: PaginationParams): Promise<ApiResponse<PaginatedResult<ContaReceberDTO>>> {
    return this.receivablesService.listContasReceber(empresaId, params);
  }

  async createContaReceber(dto: CreateContaReceberDTO): Promise<ApiResponse<ContaReceberDTO>> {
    return this.receivablesService.createContaReceber(dto);
  }

  async importXmlNfe(empresaId: string, xmlContent: string): Promise<ApiResponse<ImportXmlResponseDTO>> {
    return this.receivablesService.importXmlNfe(empresaId, xmlContent);
  }

  async listClientes(empresaId: string): Promise<ApiResponse<ClienteFornecedorDTO[]>> {
    return this.receivablesService.listClientes(empresaId);
  }
}
