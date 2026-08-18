package com.fatorr.backend.repository;

import com.fatorr.backend.domain.FolhaPagamentoMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface FolhaPagamentoMensalRepository extends JpaRepository<FolhaPagamentoMensal, Long> {
    
    @Query("SELECT f FROM FolhaPagamentoMensal f WHERE f.empresa.id = :empresaId AND f.competencia >= :dataInicio AND f.competencia <= :dataFim ORDER BY f.competencia ASC")
    List<FolhaPagamentoMensal> findUltimos12Meses(@Param("empresaId") Long empresaId, @Param("dataInicio") YearMonth dataInicio, @Param("dataFim") YearMonth dataFim);

    List<FolhaPagamentoMensal> findByEmpresaIdAndCompetencia(Long empresaId, YearMonth competencia);
}
