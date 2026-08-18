package com.fatorr.backend.repository;

import com.fatorr.backend.domain.ParametroTributario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametroTributarioRepository extends JpaRepository<ParametroTributario, Long> {
    Optional<ParametroTributario> findByAtivoTrueAndEmpresaIsNull();
    Optional<ParametroTributario> findByAtivoTrueAndEmpresaId(Long empresaId);
}
