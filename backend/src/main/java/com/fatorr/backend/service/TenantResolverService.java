package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.util.CnpjUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantResolverService {

    private final EmpresaRepository empresaRepository;

    public Empresa resolveEmpresa(String empresaId) {
        if (empresaId == null || empresaId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificador da empresa é obrigatório");
        }

        String idLimpo = empresaId.trim();

        // 1. Tentar por UUID
        Optional<Empresa> porUuid = empresaRepository.findByUuidId(idLimpo);
        if (porUuid.isPresent()) {
            return porUuid.get();
        }

        // 2. Tentar por ID numérico (Long)
        try {
            Long numericId = Long.parseLong(idLimpo);
            Optional<Empresa> porId = empresaRepository.findById(numericId);
            if (porId.isPresent()) {
                return porId.get();
            }
        } catch (NumberFormatException ignored) {
        }

        // 3. Tentar por CNPJ
        String cnpjUnmasked = CnpjUtils.unmask(idLimpo);
        if (cnpjUnmasked.length() == 14) {
            Optional<Empresa> porCnpj = empresaRepository.findByCnpj(cnpjUnmasked);
            if (porCnpj.isPresent()) {
                return porCnpj.get();
            }
        }

        throw new IllegalArgumentException("Empresa não encontrada para o identificador: " + empresaId);
    }

    public String resolveCanonicalUuid(String empresaId) {
        Empresa empresa = resolveEmpresa(empresaId);
        return empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId());
    }

    public Long resolveCanonicalNumericId(String empresaId) {
        Empresa empresa = resolveEmpresa(empresaId);
        return empresa.getId();
    }
}
