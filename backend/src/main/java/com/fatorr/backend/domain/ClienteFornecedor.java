package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes_fornecedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteFornecedor {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 36)
    private String empresaId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "cnpj_cpf", nullable = false, length = 20)
    private String cnpjCpf;

    @Column(nullable = false, length = 20)
    private String tipo; // 'CLIENTE', 'FORNECEDOR', 'AMBOS'

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
