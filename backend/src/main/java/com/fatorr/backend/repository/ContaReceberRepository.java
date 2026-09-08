package com.fatorr.backend.repository;

import com.fatorr.backend.domain.ContaReceber;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<ContaReceber> findByEmpresaIdAndStatus(String empresaId, String status, Pageable pageable);
    Page<ContaReceber> findByEmpresaId(String empresaId, Pageable pageable);
        Page<ContaReceber> findByEmpresaIdAndDataVencimentoBetween(
            String empresaId, LocalDate inicio, LocalDate fim, Pageable pageable);
        Page<ContaReceber> findByEmpresaIdAndClienteId(
            String empresaId, String clienteId, Pageable pageable);
            Page<ContaReceber> findByEmpresaIdAndClienteIdAndDataVencimentoBetween(
                String empresaId, String clienteId, LocalDate inicio, LocalDate fim, Pageable pageable);
            Page<ContaReceber> findByEmpresaIdAndStatusAndClienteIdAndDataVencimentoBetween(
                String empresaId, String status, String clienteId,
                LocalDate inicio, LocalDate fim, Pageable pageable);
        Page<ContaReceber> findByEmpresaIdAndStatusAndDataVencimentoBetween(
            String empresaId, String status, LocalDate inicio, LocalDate fim, Pageable pageable);
}
