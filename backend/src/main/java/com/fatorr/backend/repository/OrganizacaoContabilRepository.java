package com.fatorr.backend.repository;

import com.fatorr.backend.domain.OrganizacaoContabil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizacaoContabilRepository extends JpaRepository<OrganizacaoContabil, String> {
    Optional<OrganizacaoContabil> findByCnpj(String cnpj);
}
