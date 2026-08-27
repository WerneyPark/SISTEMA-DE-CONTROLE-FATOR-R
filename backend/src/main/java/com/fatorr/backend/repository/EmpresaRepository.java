package com.fatorr.backend.repository;

import com.fatorr.backend.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findByUsuariosId(Long usuarioId);
    Optional<Empresa> findByCnpj(String cnpj);
    Optional<Empresa> findByUuidId(String uuidId);
}
