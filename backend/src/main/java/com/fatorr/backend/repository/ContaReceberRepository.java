package com.fatorr.backend.repository;

import com.fatorr.backend.domain.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, String> {
    List<ContaReceber> findByEmpresaId(String empresaId);
    List<ContaReceber> findByEmpresaIdAndStatus(String empresaId, String status);
    List<ContaReceber> findByEmpresaIdAndDataVencimentoBetween(String empresaId, LocalDate inicio, LocalDate fim);
    Optional<ContaReceber> findByEmpresaIdAndXmlChaveAcesso(String empresaId, String xmlChaveAcesso);
}
