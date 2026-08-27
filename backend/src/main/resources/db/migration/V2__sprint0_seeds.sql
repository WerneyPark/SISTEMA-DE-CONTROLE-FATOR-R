-- ==============================================================================
-- SPRINT 0 DATABASE SEEDS - SISTEMA DE CONTROLE FATOR R
-- ==============================================================================

-- 1. Organização Contábil
INSERT INTO organizacoes_contabeis (id, nome, cnpj, crc, created_at, updated_at)
VALUES (
    'org-modelo-uuid-0001',
    'Organização Contábil Modelo Ltda',
    '12ABC345000199',
    'CRC-SP 123456/O',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 2. Usuário Contabilista (Admin Master)
INSERT INTO usuarios (id, nome, email, cpf, senha_hash, tipo_perfil, organizacao_contabil_id, status, created_at, updated_at)
VALUES (
    'usr-contador-uuid-0001',
    'Contador Chefe',
    'contador@teste.com',
    '111.222.333-44',
    '123456',
    'CONTABILISTA',
    'org-modelo-uuid-0001',
    'ATIVO',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 3. Duas Empresas Vinculadas à Contabilidade
INSERT INTO empresas (id, razao_social, nome_fantasia, cnpj, organizacao_contabil_id, data_inicio_atividade, created_at, updated_at)
VALUES 
(
    'emp-alpha-uuid-0001',
    'Empresa Alpha Serviços',
    'Alpha Serviços Contábeis',
    '12ABC345000101',
    'org-modelo-uuid-0001',
    '2024-01-01',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'emp-beta-uuid-0002',
    'Empresa Beta Tecnologia',
    'Beta Tech Solutions',
    '98XYZ765000102',
    'org-modelo-uuid-0001',
    '2024-01-01',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 4. Usuário Empresário vinculado a ambas as empresas com status APROVADO
INSERT INTO usuarios (id, nome, email, cpf, senha_hash, tipo_perfil, status, created_at, updated_at)
VALUES (
    'usr-empresario-uuid-0002',
    'Empresário Silva',
    'empresario@teste.com',
    '222.333.444-55',
    '123456',
    'EMPRESARIO',
    'ATIVO',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Vínculos do Empresário com Empresa Alpha e Empresa Beta
INSERT INTO empresa_usuarios (id, empresa_id, usuario_id, funcao, status_vinculo, aprovado_por, aprovado_em, created_at)
VALUES 
(
    'vinc-emp-alpha-0001',
    'emp-alpha-uuid-0001',
    'usr-empresario-uuid-0002',
    'Sócio Administrador',
    'APROVADO',
    'usr-contador-uuid-0001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'vinc-emp-beta-0002',
    'emp-beta-uuid-0002',
    'usr-empresario-uuid-0002',
    'Sócio Diretor',
    'APROVADO',
    'usr-contador-uuid-0001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 5. Usuário Colaborador Pré-Cadastrado por CPF
INSERT INTO usuarios (id, nome, email, cpf, senha_hash, tipo_perfil, status, created_at, updated_at)
VALUES (
    'usr-colaborador-uuid-0003',
    'Colaborador Convidado',
    'operador@teste.com',
    '999.888.777-66',
    NULL,
    'USUARIO',
    'PENDENTE_CADASTRO',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Vínculo do Colaborador Pré-Cadastrado na Empresa Alpha
INSERT INTO empresa_usuarios (id, empresa_id, usuario_id, funcao, status_vinculo, aprovado_por, aprovado_em, created_at)
VALUES (
    'vinc-colab-alpha-0003',
    'emp-alpha-uuid-0001',
    'usr-colaborador-uuid-0003',
    'Operador Financeiro',
    'PENDENTE',
    'usr-empresario-uuid-0002',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
