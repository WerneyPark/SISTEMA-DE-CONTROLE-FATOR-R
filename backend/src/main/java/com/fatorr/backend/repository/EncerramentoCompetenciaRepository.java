package com.fatorr.backend.repository;

import com.fatorr.backend.domain.EncerramentoCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface EncerramentoCompetenciaRepository extends JpaRepository<EncerramentoCompetencia, Long> {

    Optional<EncerramentoCompetencia> findByEmpresaIdAndCompetencia(Long empresaId, YearMonth competencia);

    List<EncerramentoCompetencia> findByEmpresaIdAndEncerradoTrue(Long empresaId);

    List<EncerramentoCompetencia> findByEmpresaId(Long empresaId);
}
