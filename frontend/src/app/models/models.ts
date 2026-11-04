export interface Empresa {
  id?: number;
  uuidId?: string;
  razaoSocial: string;
  cnpj: string;
  dataInicioAtividade: string;
}

export interface FaturamentoMensal {
  id?: number;
  empresaId: number;
  competencia: string; // Ex: "2023-01"
  valor: number;
}

export interface FolhaPagamentoMensal {
  id?: number;
  empresaId: number;
  competencia: string; // Ex: "2023-01"
  valor: number;
}

export interface ParametroTributario {
  id?: number;
  aliquotaPis: number;
  aliquotaCofins: number;
  aliquotaIss: number;
  aliquotaIrpj: number;
  aliquotaAdicionalIrpj: number;
  aliquotaCsll: number;
  faixas: FaixaTributaria[];
  ativo: boolean;
}

export interface AuthResponse {
  id: number;
  uuidId?: string;
  cpf: string;
  tipo: 'ADMINISTRADOR_CONTABIL' | 'USUARIO_EMPRESA';
  token: string;
}

export interface SimulacaoTributaria {
  empresaId: number;
  razaoSocial: string;
  cnpj: string;
  competenciaReferencia: string;
  mesesComDados: number;
  totalFaturamento12Meses: number;
  totalFolha12Meses: number;
  rbt12: number;
  faixaEncontrada: number;
  mensagemFaixa6: string;
  percentualFatorR: number;
  anexoAplicavel: string;
  alertaProximidadeLimite: boolean;
  mensagemAlerta: string;
  aliquotaEfetivaSimplesNacional: number;
  valorEstimadoSimplesNacional: number;
  aliquotaEfetivaLucroPresumido: number;
  valorEstimadoLucroPresumido: number;
  valorPis: number;
  valorCofins: number;
  valorIss: number;
  valorIrpj: number;
  valorCsll: number;
  valorAdicionalIrpj: number;
  economiaEstimada: number;
  regimeMaisVantajoso: string;
  mensagem: string;
  historicoFaturamentos: FaturamentoHistorico[];
  competenciaReferenciaEncerrada?: boolean;
  competenciasEncerradas?: string[];
}

export interface FaixaTributaria {
  id?: number;
  anexo: string;
  numeroFaixa: number;
  limiteSuperior?: number;
  aliquotaNominal: number;
  valorDeduzir: number;
}

export interface FaturamentoHistorico {
  competencia: string;
  faturamento: number;
  folhaPagamento: number;
  encerrado?: boolean;
}

export interface MesProjecao {
  competencia: string;
  faturamentoEstimado: number;
  folhaEstimada: number;
  rbt12Estimado: number;
  percentualFatorR: number;
  anexoAplicavel: string;
  valorEstimadoSimplesNacional: number;
}

export interface ProjecaoCenario {
  nomeCenario: string;
  descricao: string;
  meses: MesProjecao[];
}

export interface EmpresaResumo {
  id: number;
  razaoSocial: string;
  cnpj: string;
}

export interface UsuarioAdmin {
  id: number;
  cpf: string;
  tipo: string;
  empresas: EmpresaResumo[];
}
