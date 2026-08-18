package com.fatorr.backend.repository;

import com.fatorr.backend.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCpf(String cpf);
    java.util.List<Usuario> findByEmpresasId(Long empresaId);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresas")
    java.util.List<Usuario> findAllWithEmpresas();
}
