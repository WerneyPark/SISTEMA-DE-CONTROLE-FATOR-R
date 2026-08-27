package com.fatorr.backend.repository;

import com.fatorr.backend.domain.ClienteFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteFornecedorRepository extends JpaRepository<ClienteFornecedor, String> {
    List<ClienteFornecedor> findByEmpresaId(String empresaId);
    List<ClienteFornecedor> findByEmpresaIdAndTipo(String empresaId, String tipo);
    Optional<ClienteFornecedor> findByEmpresaIdAndCnpjCpf(String empresaId, String cnpjCpf);
}
