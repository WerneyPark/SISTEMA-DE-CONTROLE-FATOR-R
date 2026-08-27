import { AuthController } from '../controllers/auth.controller';

export interface RouteDefinition {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  path: string;
  handler: string;
  roles?: string[];
}

export const authRoutes: RouteDefinition[] = [
  { method: 'POST', path: '/api/auth/login', handler: 'login' },
  { method: 'POST', path: '/api/auth/register/contador', handler: 'registerContador' },
  { method: 'POST', path: '/api/auth/register/empresario', handler: 'registerEmpresario' },
  { method: 'POST', path: '/api/auth/register/usuario', handler: 'registerUsuario' },
  { method: 'POST', path: '/api/auth/convidar', handler: 'inviteUserByCpf', roles: ['CONTABILISTA', 'EMPRESARIO'] },
  { method: 'PATCH', path: '/api/auth/vinculos/aprovar', handler: 'approveUserVinculo', roles: ['CONTABILISTA', 'EMPRESARIO'] },
  { method: 'POST', path: '/api/auth/forgot-password', handler: 'forgotPassword' },
  { method: 'POST', path: '/api/auth/reset-password', handler: 'resetPassword' }
];
