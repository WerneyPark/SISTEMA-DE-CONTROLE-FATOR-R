package com.fatorr.backend.repository;

import com.fatorr.backend.domain.ExtratoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExtratoLancamentoRepository extends JpaRepository<ExtratoLancamento, String> {
    List<ExtratoLancamento> findByEmpresaId(String empresaId);
    List<ExtratoLancamento> findByEmpresaIdAndStatusConciliacao(String empresaId, String statusConciliacao);
    Optional<ExtratoLancamento> findByEmpresaIdAndFitid(String empresaId, String fitid);
    List<ExtratoLancamento> findByEmpresaIdAndDataLancamentoBetween(String empresaId, LocalDate inicio, LocalDate fim);
}
