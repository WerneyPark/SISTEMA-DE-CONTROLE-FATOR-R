import { RouteDefinition } from '../../auth/routes/auth.routes';

export const receivablesRoutes: RouteDefinition[] = [
  { method: 'GET', path: '/api/receivables/:empresaId', handler: 'listContasReceber' },
  { method: 'POST', path: '/api/receivables', handler: 'createContaReceber' },
  { method: 'POST', path: '/api/receivables/:empresaId/import-xml', handler: 'importXmlNfe' },
  { method: 'GET', path: '/api/receivables/:empresaId/clientes', handler: 'listClientes' }
];
