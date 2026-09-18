-- ==============================================================================
-- SPRINT 0 (EXPANSÃO) - FOLHA DETALHADA E LAUDOS TRIBUTÁRIOS (TRILHAS 4 E 5)
-- ==============================================================================

-- 9. Folha de Pagamento Detalhada (Trilha 4 - eSocial / ERP Contábil)
CREATE TABLE IF NOT EXISTS folhas_detalhadas (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(36) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    competencia VARCHAR(7) NOT NULL, -- Ex: '2026-08'
    valor_salarios NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_pro_labore NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_decimo_terceiro NUMERIC(15, 2) DEFAULT 0.00,
    valor_inss_patronal NUMERIC(15, 2) DEFAULT 0.00,
    valor_fgts NUMERIC(15, 2) DEFAULT 0.00,
    valor_total NUMERIC(15, 2) NOT NULL,
    origem VARCHAR(50) DEFAULT 'MANUAL', -- 'MANUAL', 'IMPORTACAO_ESOCIAL', 'IMPORTACAO_CSV'
    xml_chave_recibo VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (empresa_id, competencia)
);

-- 10. Laudos Tributários e Dossiês Oficiais (Trilha 5 - Pareceres e Auditoria)
CREATE TABLE IF NOT EXISTS laudos_tributarios (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(36) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    usuario_id VARCHAR(36) NOT NULL REFERENCES usuarios(id),
    competencia VARCHAR(7) NOT NULL,
    tipo_laudo VARCHAR(50) NOT NULL,
    hash_autenticidade VARCHAR(100) UNIQUE NOT NULL,
    caminho_arquivo VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
