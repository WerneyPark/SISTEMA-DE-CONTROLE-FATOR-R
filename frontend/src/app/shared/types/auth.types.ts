export type TipoPerfil = 'CONTABILISTA' | 'EMPRESARIO' | 'USUARIO';
export type StatusVinculo = 'PENDENTE' | 'APROVADO' | 'REJEITADO';
export type StatusUsuario = 'ATIVO' | 'PENDENTE_CADASTRO' | 'INATIVO';

export interface RegisterContadorDTO {
  nome: string;
  email: string;
  cpf: string;
  senha: string;
  crc: string;
  organizacaoNome: string;
  organizacaoCnpj: string; // Alfanumérico
}

export interface RegisterEmpresarioDTO {
  nome: string;
  email: string;
  cpf: string;
  senha: string;
  cnpjs: string[]; // Múltiplos CNPJs alfanuméricos
}

export interface RegisterUsuarioDTO {
  nome: string;
  email: string;
  cpf: string;
  senha: string;
  cnpjEmpresa: string;
}

export interface InviteUserByCpfDTO {
  cpf: string;
  funcao: string;
  empresaId: string;
}

export interface ApproveUserVinculoDTO {
  empresaUsuarioId: string;
  status: 'APROVADO' | 'REJEITADO';
}

export interface ForgotPasswordDTO {
  email: string;
}

export interface ResetPasswordDTO {
  token: string;
  novaSenha: string;
}

export interface LoginRequestDTO {
  login: string; // CPF ou Email
  senha: string;
}

export interface LoginResponseDTO {
  token: string;
  usuario: {
    id: string;
    nome: string;
    email: string;
    cpf: string;
    tipoPerfil: TipoPerfil;
    status: StatusUsuario;
    organizacaoContabilId?: string;
  };
  empresas: Array<{
    id: string;
    razaoSocial: string;
    cnpj: string;
    funcao?: string;
    statusVinculo?: StatusVinculo;
  }>;
}
