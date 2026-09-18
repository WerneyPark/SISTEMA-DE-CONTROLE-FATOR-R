import { RouteDefinition } from '../../auth/routes/auth.routes';

export const payrollRoutes: RouteDefinition[] = [
  { method: 'GET', path: '/api/payroll/:empresaId', handler: 'listFolhas' },
  { method: 'GET', path: '/api/payroll/:empresaId/competencia/:competencia', handler: 'getFolhaByCompetencia' },
  { method: 'POST', path: '/api/payroll', handler: 'saveFolha' },
  { method: 'POST', path: '/api/payroll/:empresaId/import-esocial', handler: 'importEsocialXml' },
  { method: 'POST', path: '/api/payroll/:empresaId/import-csv', handler: 'importFolhaCsv' },
  { method: 'GET', path: '/api/payroll/:empresaId/pro-labore-ideal', handler: 'calcularProLaboreIdeal' }
];
