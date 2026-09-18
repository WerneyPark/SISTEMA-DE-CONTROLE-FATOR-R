package com.fatorr.backend.repository;

import com.fatorr.backend.domain.FolhaDetalhada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolhaDetalhadaRepository extends JpaRepository<FolhaDetalhada, String> {
    List<FolhaDetalhada> findByEmpresaId(String empresaId);
    Optional<FolhaDetalhada> findByEmpresaIdAndCompetencia(String empresaId, String competencia);
    List<FolhaDetalhada> findByEmpresaIdAndOrigem(String empresaId, String origem);
}
