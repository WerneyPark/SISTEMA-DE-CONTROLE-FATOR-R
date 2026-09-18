package com.fatorr.backend.repository;

import com.fatorr.backend.domain.LaudoTributario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaudoTributarioRepository extends JpaRepository<LaudoTributario, String> {
    List<LaudoTributario> findByEmpresaId(String empresaId);
    Optional<LaudoTributario> findByHashAutenticidade(String hashAutenticidade);
    List<LaudoTributario> findByEmpresaIdAndCompetencia(String empresaId, String competencia);
    List<LaudoTributario> findByUsuarioId(String usuarioId);
}
