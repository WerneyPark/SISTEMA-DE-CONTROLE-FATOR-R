package com.fatorr.backend.repository;

import com.fatorr.backend.domain.ConciliacaoBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConciliacaoBancariaRepository extends JpaRepository<ConciliacaoBancaria, String> {
    Optional<ConciliacaoBancaria> findByExtratoLancamentoId(String extratoLancamentoId);
    List<ConciliacaoBancaria> findByContaReceberId(String contaReceberId);
    List<ConciliacaoBancaria> findByUsuarioId(String usuarioId);
}
