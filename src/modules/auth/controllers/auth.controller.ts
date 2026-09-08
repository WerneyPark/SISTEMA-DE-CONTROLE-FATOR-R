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
import { BaixarContaReceberDTO } from '../../../shared/types/receivables.types';
import { AuthService } from '../services/auth.service';

export class AuthController {
  constructor(private readonly authService: AuthService = new AuthService()) {}

  async login(dto: LoginRequestDTO): Promise<ApiResponse<LoginResponseDTO>> {
    return this.authService.login(dto);
  }

  async registerContador(dto: RegisterContadorDTO): Promise<ApiResponse<{ usuarioId: string; organizacaoId: string }>> {
    return this.authService.registerContador(dto);
  }

  async registerEmpresario(dto: RegisterEmpresarioDTO): Promise<ApiResponse<{ usuarioId: string; empresasCadastradas: number }>> {
    return this.authService.registerEmpresario(dto);
  }

  async registerUsuario(dto: RegisterUsuarioDTO): Promise<ApiResponse<{ usuarioId: string }>> {
    return this.authService.registerUsuario(dto);
  }

  async inviteUserByCpf(dto: InviteUserByCpfDTO): Promise<ApiResponse<{ vinculoId: string }>> {
    return this.authService.inviteUserByCpf(dto);
  }

  async approveUserVinculo(dto: ApproveUserVinculoDTO): Promise<ApiResponse<{ status: string }>> {
    return this.authService.approveUserVinculo(dto);
  }

  async forgotPassword(dto: ForgotPasswordDTO): Promise<ApiResponse<void>> {
    return this.authService.forgotPassword(dto);
  }

  async resetPassword(dto: ResetPasswordDTO): Promise<ApiResponse<void>> {
    return this.authService.resetPassword(dto);
  }
}
