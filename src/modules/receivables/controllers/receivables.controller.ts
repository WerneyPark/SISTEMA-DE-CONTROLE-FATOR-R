import { 
  ContaReceberDTO, 
  InclusaoManualContaReceberDTO,
  ResultadoImportacaoDTO,
  PreviewXmlDTO,
  PreviewXmlRequestDTO,
  FiltrosContasReceberDTO,
  RespostaPaginadaContasReceberDTO,
  ClienteFornecedorDTO,
  BaixarContaReceberDTO
} from '../../../shared/types/receivables.types';
import { ApiResponse } from '../../../shared/types/common.types';
import { ReceivablesService } from '../services/receivables.service';

export class ReceivablesController {
  constructor(private readonly receivablesService: ReceivablesService = new ReceivablesService()) {}

  async listContasReceber(filtros: FiltrosContasReceberDTO): Promise<ApiResponse<RespostaPaginadaContasReceberDTO>> {
    return this.receivablesService.listContasReceber(filtros);
  }

  async createContaReceber(dto: InclusaoManualContaReceberDTO): Promise<ApiResponse<ContaReceberDTO>> {
    return this.receivablesService.createContaReceber(dto);
  }

  async previewXml(dto: PreviewXmlRequestDTO): Promise<ApiResponse<PreviewXmlDTO>> {
    return this.receivablesService.previewXml(dto);
  }

  async importXmlNfe(empresaId: string, xmlContent: string): Promise<ApiResponse<ResultadoImportacaoDTO>> {
    return this.receivablesService.importXmlNfe(empresaId, xmlContent);
  }

  async listClientes(empresaId: string): Promise<ApiResponse<ClienteFornecedorDTO[]>> {
    return this.receivablesService.listClientes(empresaId);
  }

  async baixarContaReceber(dto: BaixarContaReceberDTO): Promise<ApiResponse<ContaReceberDTO>> {
    return this.receivablesService.baixarContaReceber(dto);
  }
}
