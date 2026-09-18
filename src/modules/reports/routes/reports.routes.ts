import { RouteDefinition } from '../../auth/routes/auth.routes';

export const reportsRoutes: RouteDefinition[] = [
  { method: 'POST', path: '/api/reports/emitir', handler: 'emitirLaudo' },
  { method: 'GET', path: '/api/reports/:empresaId/historico', handler: 'listLaudos' },
  { method: 'GET', path: '/api/reports/validar/:hash', handler: 'validarAutenticidade' },
  { method: 'GET', path: '/api/reports/:empresaId/excel/:competencia', handler: 'exportarExcelAnalitico' }
];
