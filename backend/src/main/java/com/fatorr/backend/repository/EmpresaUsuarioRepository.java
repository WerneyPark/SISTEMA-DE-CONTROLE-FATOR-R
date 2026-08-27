package com.fatorr.backend.repository;

import com.fatorr.backend.domain.EmpresaUsuario;
import com.fatorr.backend.domain.StatusVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaUsuarioRepository extends JpaRepository<EmpresaUsuario, String> {
    List<EmpresaUsuario> findByEmpresaId(String empresaId);
    List<EmpresaUsuario> findByUsuarioId(String usuarioId);
    Optional<EmpresaUsuario> findByEmpresaIdAndUsuarioId(String empresaId, String usuarioId);
    List<EmpresaUsuario> findByEmpresaIdAndStatusVinculo(String empresaId, StatusVinculo statusVinculo);
}
