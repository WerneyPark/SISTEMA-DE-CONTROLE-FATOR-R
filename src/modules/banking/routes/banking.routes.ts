import { RouteDefinition } from '../../auth/routes/auth.routes';

export const bankingRoutes: RouteDefinition[] = [
  { method: 'GET', path: '/api/banking/:empresaId/lancamentos', handler: 'listLancamentos' },
  { method: 'POST', path: '/api/banking/:empresaId/import-extrato', handler: 'importExtrato' },
  { method: 'POST', path: '/api/banking/conciliar', handler: 'conciliarManual' },
  { method: 'GET', path: '/api/banking/:empresaId/conciliacoes', handler: 'listConciliacoes' }
];
