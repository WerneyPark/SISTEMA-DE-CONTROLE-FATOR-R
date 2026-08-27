import { TipoPerfil } from '../types/auth.types';

export interface AuthenticatedUser {
  id: string;
  nome: string;
  email: string;
  cpf: string;
  tipoPerfil: TipoPerfil;
  organizacaoContabilId?: string;
}

export interface RequestWithUser {
  headers: Record<string, string | undefined>;
  user?: AuthenticatedUser;
}

/**
 * Middleware de validação de autenticação e RBAC
 */
export function validateAuthToken(token: string): AuthenticatedUser | null {
  if (!token) return null;
  // Stub de verificação de token JWT/Bearer
  return {
    id: 'user-auth-stub',
    nome: 'Usuário Autenticado',
    email: 'auth@fatorr.com.br',
    cpf: '00000000000',
    tipoPerfil: 'CONTABILISTA'
  };
}

export function authorizeRoles(...allowedRoles: TipoPerfil[]) {
  return (user?: AuthenticatedUser): boolean => {
    if (!user) return false;
    return allowedRoles.includes(user.tipoPerfil);
  };
}
