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

export class ReceivablesService {
  private readonly contasReceber: ContaReceberDTO[] = [];

  async listContasReceber(
    filtros: FiltrosContasReceberDTO
  ): Promise<ApiResponse<RespostaPaginadaContasReceberDTO>> {
    const page = Math.max(filtros.page || 1, 1);
    const limit = Math.max(filtros.limit || 10, 1);
    const filtradas = this.contasReceber.filter(conta => {
      const dentroDoPeriodo = (!filtros.dataVencimentoInicio || conta.dataVencimento >= filtros.dataVencimentoInicio)
        && (!filtros.dataVencimentoFim || conta.dataVencimento <= filtros.dataVencimentoFim);

      return conta.empresaId === filtros.empresaId
        && (!filtros.status || conta.status === filtros.status)
        && (!filtros.clienteId || conta.clienteId === filtros.clienteId)
        && dentroDoPeriodo;
    });

    const inicio = (page - 1) * limit;
    const items = filtradas.slice(inicio, inicio + limit);

    return {
      success: true,
      data: {
        items,
        total: filtradas.length,
        page,
        limit,
        totalPages: Math.ceil(filtradas.length / limit)
      }
    };
  }

  async createContaReceber(dto: InclusaoManualContaReceberDTO): Promise<ApiResponse<ContaReceberDTO>> {
    const novaConta: ContaReceberDTO = {
      id: 'rec-' + Date.now(),
      empresaId: dto.empresaId,
      clienteId: dto.clienteId,
      numeroDocumento: dto.numeroDocumento,
      valorTotal: dto.valorTotal,
      dataEmissao: dto.dataEmissao,
      dataVencimento: dto.dataVencimento,
      status: 'PENDENTE',
      origem: 'MANUAL',
      createdAt: new Date().toISOString()
    };

    this.contasReceber.push(novaConta);

    return {
      success: true,
      data: novaConta
    };
  }

  async baixarContaReceber(dto: BaixarContaReceberDTO): Promise<ApiResponse<ContaReceberDTO>> {
    const conta = this.contasReceber.find(item => item.id === dto.contaReceberId);

    if (!conta) {
      return {
        success: false,
        error: 'Conta a receber não encontrada.'
      };
    }

    if (conta.status === 'PAGO') {
      return {
        success: false,
        error: 'A conta a receber já está paga.'
      };
    }

    if (conta.status === 'CANCELADO') {
      return {
        success: false,
        error: 'Uma conta cancelada não pode ser baixada.'
      };
    }

    const dataPagamento = new Date(dto.dataPagamento);

    if (Number.isNaN(dataPagamento.getTime())) {
      return {
        success: false,
        error: 'A data de pagamento é inválida.'
      };
    }

    conta.status = 'PAGO';
    conta.dataPagamento = dto.dataPagamento;
    conta.updatedAt = new Date().toISOString();

    return {
      success: true,
      message: 'Conta a receber baixada com sucesso.',
      data: conta
    };
  }

  async previewXml(dto: PreviewXmlRequestDTO): Promise<ApiResponse<PreviewXmlDTO>> {
    if (!dto.xmlContent.trim()) {
      return {
        success: false,
        error: 'O conteúdo do XML não pode estar vazio.'
      };
    }

    return {
      success: true,
      data: {
        empresaId: dto.empresaId,
        tipoDocumento: 'NFE',
        chaveAcesso: '35240112ABC345000101550010000001231000001234',
        numeroNota: '123',
        dataEmissao: '2026-08-20',
        emitente: {
          nome: 'Empresa Emitente SA',
          cnpj: '12ABC345000101'
        },
        tomador: {
          nome: 'Cliente Modelo SA',
          cnpj: '98XYZ765000199',
          clienteExistente: false
        },
        valorTotal: 15000,
        parcelas: [
          {
            numero: '01',
            valor: 15000,
            dataVencimento: '2026-09-20'
          }
        ]
      }
    };
  }

  async importXmlNfe(empresaId: string, xmlContent: string): Promise<ApiResponse<ResultadoImportacaoDTO>> {
    // Scaffold para parser de XML de NFe/NFS-e
    const previewResponse = await this.previewXml({ empresaId, xmlContent });

    if (!previewResponse.success || !previewResponse.data) {
      return {
        success: false,
        error: previewResponse.error || 'Não foi possível gerar o preview do XML.'
      };
    }

    const preview = previewResponse.data;
    const clienteId = 'cli-001';
    const titulo: ContaReceberDTO = {
      id: 'rec-xml-' + Date.now(),
      empresaId,
      clienteId,
      clienteNome: preview.tomador.nome,
      clienteCnpj: preview.tomador.cnpj,
      numeroDocumento: `${preview.numeroNota}/${preview.parcelas[0].numero}`,
      valorTotal: preview.parcelas[0].valor,
      dataEmissao: preview.dataEmissao,
      dataVencimento: preview.parcelas[0].dataVencimento,
      status: 'PENDENTE',
      origem: 'IMPORTACAO_XML',
      xmlChaveAcesso: preview.chaveAcesso,
      createdAt: new Date().toISOString()
    };

    this.contasReceber.push(titulo);

    return {
      success: true,
      message: 'XML processado com sucesso.',
      data: {
        sucesso: true,
        mensagem: 'XML processado com sucesso.',
        preview,
        clienteCriado: true,
        clienteId,
        titulosGerados: [titulo],
        duplicadosIgnorados: 0
      }
    };
  }

  async listClientes(empresaId: string): Promise<ApiResponse<ClienteFornecedorDTO[]>> {
    return {
      success: true,
      data: []
    };
  }
}
