import { 
  RegisterContadorDTO, 
  RegisterEmpresarioDTO, 
  RegisterUsuarioDTO, 
  InviteUserByCpfDTO, 
  ApproveUserVinculoDTO, 
  ForgotPasswordDTO, 
  ResetPasswordDTO, 
  LoginRequestDTO, 
  LoginResponseDTO 
} from '../../../shared/types/auth.types';
import { ApiResponse } from '../../../shared/types/common.types';
import { unmaskCnpj, isValidCnpj } from '../../../shared/utils/cnpj';
import { unmaskCpf, isValidCpf } from '../../../shared/utils/formatters';

export class AuthService {
  async login(dto: LoginRequestDTO): Promise<ApiResponse<LoginResponseDTO>> {
    // Scaffold - pronto para implementação completa pela Trilha 1
    return {
      success: true,
      data: {
        token: 'jwt-token-scaffold',
        usuario: {
          id: 'usr-scaffold-001',
          nome: 'Usuário Teste',
          email: 'teste@fatorr.com.br',
          cpf: dto.login,
          tipoPerfil: 'CONTABILISTA',
          status: 'ATIVO'
        },
        empresas: []
      }
    };
  }

  async registerContador(dto: RegisterContadorDTO): Promise<ApiResponse<{ usuarioId: string; organizacaoId: string }>> {
    const cleanCnpj = unmaskCnpj(dto.organizacaoCnpj);
    if (!isValidCnpj(cleanCnpj)) {
      return { success: false, error: 'CNPJ da Organização Contábil inválido.' };
    }
    const cleanCpf = unmaskCpf(dto.cpf);
    if (!isValidCpf(cleanCpf)) {
      return { success: false, error: 'CPF do Contador inválido.' };
    }

    return {
      success: true,
      message: 'Organização Contábil e Contador cadastrados com sucesso.',
      data: {
        usuarioId: 'usr-' + Date.now(),
        organizacaoId: 'org-' + Date.now()
      }
    };
  }

  async registerEmpresario(dto: RegisterEmpresarioDTO): Promise<ApiResponse<{ usuarioId: string; empresasCadastradas: number }>> {
    for (const cnpj of dto.cnpjs) {
      if (!isValidCnpj(unmaskCnpj(cnpj))) {
        return { success: false, error: `CNPJ inválido: ${cnpj}` };
      }
    }
    return {
      success: true,
      message: 'Empresário cadastrado e vinculado às empresas.',
      data: {
        usuarioId: 'usr-' + Date.now(),
        empresasCadastradas: dto.cnpjs.length
      }
    };
  }

  async registerUsuario(dto: RegisterUsuarioDTO): Promise<ApiResponse<{ usuarioId: string }>> {
    return {
      success: true,
      message: 'Usuário colaborador cadastrado com sucesso.',
      data: { usuarioId: 'usr-' + Date.now() }
    };
  }

  async inviteUserByCpf(dto: InviteUserByCpfDTO): Promise<ApiResponse<{ vinculoId: string }>> {
    return {
      success: true,
      message: 'Convite enviado / vínculo pendente registrado.',
      data: { vinculoId: 'vinc-' + Date.now() }
    };
  }

  async approveUserVinculo(dto: ApproveUserVinculoDTO): Promise<ApiResponse<{ status: string }>> {
    return {
      success: true,
      message: `Vínculo atualizado para ${dto.status}.`,
      data: { status: dto.status }
    };
  }

  async forgotPassword(dto: ForgotPasswordDTO): Promise<ApiResponse<void>> {
    return {
      success: true,
      message: 'Email de recuperação enviado caso o usuário exista.'
    };
  }

  async resetPassword(dto: ResetPasswordDTO): Promise<ApiResponse<void>> {
    return {
      success: true,
      message: 'Senha redefinida com sucesso.'
    };
  }
}
