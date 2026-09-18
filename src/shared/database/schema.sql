-- ==============================================================================
-- SISTEMA DE CONTROLE FATOR R - SPRINT 0 DATABASE SCHEMA (PostgreSQL)
-- ==============================================================================

-- 1. Organizações Contábeis (Escritórios de Contabilidade)
CREATE TABLE IF NOT EXISTS organizacoes_contabeis (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20) NOT NULL,
    crc VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Usuários do Sistema
CREATE TABLE IF NOT EXISTS usuarios (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    senha_hash VARCHAR(255),
    tipo_perfil VARCHAR(50) NOT NULL, -- 'CONTABILISTA', 'EMPRESARIO', 'USUARIO'
    organizacao_contabil_id VARCHAR(36) REFERENCES organizacoes_contabeis(id),
    status VARCHAR(30) DEFAULT 'ATIVO', -- 'ATIVO', 'PENDENTE_CADASTRO', 'INATIVO'
    reset_password_token VARCHAR(255),
    reset_password_expires TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Empresas (Tenants - CNPJ Alfanumérico)
CREATE TABLE IF NOT EXISTS empresas (
    id VARCHAR(36) PRIMARY KEY,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(20) UNIQUE NOT NULL, -- VARCHAR para suporte ao novo padrão alfanumérico
    organizacao_contabil_id VARCHAR(36) REFERENCES organizacoes_contabeis(id),
    data_inicio_atividade DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Vínculo NxN Usuário <-> Empresa (Controle de Acesso e Convites)
CREATE TABLE IF NOT EXISTS empresa_usuarios (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(36) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    usuario_id VARCHAR(36) NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    funcao VARCHAR(100) NOT NULL,
    status_vinculo VARCHAR(30) DEFAULT 'PENDENTE', -- 'PENDENTE', 'APROVADO', 'REJEITADO'
    aprovado_por VARCHAR(36) REFERENCES usuarios(id),
    aprovado_em TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (empresa_id, usuario_id)
);

-- 5. Clientes / Fornecedores (Tomadores e Prestadores de Serviços)
CREATE TABLE IF NOT EXISTS clientes_fornecedores (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(36) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    cnpj_cpf VARCHAR(20) NOT NULL, -- Alfanumérico
    tipo VARCHAR(20) NOT NULL, -- 'CLIENTE', 'FORNECEDOR', 'AMBOS'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Contas a Receber
CREATE TABLE IF NOT EXISTS contas_a_receber (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(36) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    cliente_id VARCHAR(36) NOT NULL REFERENCES clientes_fornecedores(id),
    numero_documento VARCHAR(100),
    valor_total NUMERIC(15, 2) NOT NULL,
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(30) DEFAULT 'PENDENTE', -- 'PENDENTE', 'PAGO', 'CANCELADO', 'VENCIDO'
    origem VARCHAR(50) DEFAULT 'MANUAL', -- 'MANUAL', 'IMPORTACAO_XML'
    xml_chave_acesso VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Lançamentos Bancários do Extrato
CREATE TABLE IF NOT EXISTS extrato_lancamentos (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(36) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    banco VARCHAR(100),
    conta VARCHAR(50),
    data_lancamento DATE NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    tipo VARCHAR(10) NOT NULL, -- 'CREDITO', 'DEBITO'
    fitid VARCHAR(100), -- ID único do lançamento no OFX para evitar duplicidade
    status_conciliacao VARCHAR(30) DEFAULT 'PENDENTE', -- 'PENDENTE', 'CONCILIADO'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Conciliação Bancária Manual
CREATE TABLE IF NOT EXISTS conciliacoes_bancarias (
    id VARCHAR(36) PRIMARY KEY,
    extrato_lancamento_id VARCHAR(36) NOT NULL REFERENCES extrato_lancamentos(id),
    conta_receber_id VARCHAR(36) REFERENCES contas_a_receber(id),
    usuario_id VARCHAR(36) NOT NULL REFERENCES usuarios(id),
    data_conciliacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observacao TEXT
);

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

