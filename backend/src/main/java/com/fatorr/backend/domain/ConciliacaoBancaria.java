package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "conciliacoes_bancarias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConciliacaoBancaria {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "extrato_lancamento_id", nullable = false, length = 36)
    private String extratoLancamentoId;

    @Column(name = "conta_receber_id", length = 36)
    private String contaReceberId;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    @Column(name = "data_conciliacao")
    private LocalDateTime dataConciliacao = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String observacao;
}
