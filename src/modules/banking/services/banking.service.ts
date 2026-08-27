import { 
  ExtratoLancamentoDTO, 
  ConciliarManualDTO, 
  ConciliacaoBancariaDTO, 
  ImportExtratoResponseDTO 
} from '../../../shared/types/banking.types';
import { ApiResponse, PaginatedResult, PaginationParams } from '../../../shared/types/common.types';

export class BankingService {
  async listLancamentos(empresaId: string, params?: PaginationParams): Promise<ApiResponse<PaginatedResult<ExtratoLancamentoDTO>>> {
    return {
      success: true,
      data: {
        items: [],
        total: 0,
        page: params?.page || 1,
        limit: params?.limit || 10,
        totalPages: 0
      }
    };
  }

  async importExtrato(empresaId: string, fileContent: string, formato: 'OFX' | 'CSV'): Promise<ApiResponse<ImportExtratoResponseDTO>> {
    // Scaffold para parser OFX e CSV de extratos bancários
    return {
      success: true,
      message: `Extrato ${formato} importado com sucesso.`,
      data: {
        sucesso: true,
        banco: 'Banco Itaú (341)',
        conta: '12345-6',
        totalLancamentos: 1,
        novosLancamentos: 1,
        duplicadosIgnorados: 0,
        lancamentos: [
          {
            id: 'lan-' + Date.now(),
            empresaId,
            banco: 'Banco Itaú (341)',
            conta: '12345-6',
            dataLancamento: new Date().toISOString().split('T')[0],
            descricao: 'TED RECEBIDA CLIENTE MODELO',
            valor: 15000.00,
            tipo: 'CREDITO',
            fitid: 'FITID-20260820-001',
            statusConciliacao: 'PENDENTE',
            createdAt: new Date().toISOString()
          }
        ]
      }
    };
  }

  async conciliarManual(dto: ConciliarManualDTO, usuarioId: string): Promise<ApiResponse<ConciliacaoBancariaDTO>> {
    const conciliacao: ConciliacaoBancariaDTO = {
      id: 'conc-' + Date.now(),
      extratoLancamentoId: dto.extratoLancamentoId,
      contaReceberId: dto.contaReceberId,
      usuarioId,
      dataConciliacao: new Date().toISOString(),
      observacao: dto.observacao
    };

    return {
      success: true,
      message: 'Lançamento conciliado com sucesso.',
      data: conciliacao
    };
  }

  async listConciliacoes(empresaId: string): Promise<ApiResponse<ConciliacaoBancariaDTO[]>> {
    return {
      success: true,
      data: []
    };
  }
}
