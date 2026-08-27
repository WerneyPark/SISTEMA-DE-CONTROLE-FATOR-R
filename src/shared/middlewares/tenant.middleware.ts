/**
 * Middleware para identificação e isolamento multi-tenant (por Empresa / Tenant ID)
 */

export interface TenantContext {
  empresaId?: string;
  organizacaoContabilId?: string;
}

export function extractTenantId(headers: Record<string, string | undefined>): string | null {
  return headers['x-tenant-id'] || headers['x-empresa-id'] || null;
}

export function validateTenantAccess(
  userEmpresasIds: string[],
  requestedEmpresaId: string
): boolean {
  if (!requestedEmpresaId) return false;
  return userEmpresasIds.includes(requestedEmpresaId);
}
