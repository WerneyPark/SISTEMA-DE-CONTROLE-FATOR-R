package com.fatorr.backend.repository;

import com.fatorr.backend.domain.FaturamentoMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface FaturamentoMensalRepository extends JpaRepository<FaturamentoMensal, Long> {
    
    @Query("SELECT f FROM FaturamentoMensal f WHERE f.empresa.id = :empresaId AND f.competencia >= :dataInicio AND f.competencia <= :dataFim ORDER BY f.competencia ASC")
    List<FaturamentoMensal> findUltimos12Meses(@Param("empresaId") Long empresaId, @Param("dataInicio") YearMonth dataInicio, @Param("dataFim") YearMonth dataFim);

    List<FaturamentoMensal> findByEmpresaIdAndCompetencia(Long empresaId, YearMonth competencia);
}
