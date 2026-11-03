# 📖 GUIA DE DESENVOLVIMENTO E ARQUITETURA CONSOLIDADA

Este documento contém o roadmap, as regras de arquitetura e o status de implementação das funcionalidades do **Sistema de Controle Fator R**.

---

## 🏛️ REGRAS GERAIS DE ARQUITETURA OFICIAL (JAVA 21 + SPRING BOOT 4 + ANGULAR 21)

1. **Stack Tecnológica Consolidada**:
   - **Backend**: Java 21 LTS / Spring Boot 4.0.6 em `backend/src/main/java/com/fatorr/backend/`
   - **Frontend**: Angular 21.2 (Standalone Components, RxJS) em `frontend/src/app/`
   - **Banco de Dados**: PostgreSQL 16 (Docker Compose)
   - **Persistência**: Hibernate ORM com suporte a multi-tenancy e `TenantResolverService`.

2. **CNPJ Alfanumérico (Padrão RFB)**:
   - Todo campo de CNPJ no banco, backend e frontend é do tipo `String / VARCHAR(20)`.
   - Utilize sempre as funções de utilitários:
     - Java: `com.fatorr.backend.util.CnpjUtils` (`mask`, `unmask`, `isValid`)
     - TypeScript: `frontend/src/app/shared/utils/cnpj.ts` (`maskCnpj`, `unmaskCnpj`, `isValidCnpj`)

3. **CPF Estritamente Numérico**:
   - Armazenado no banco com 11 dígitos numéricos limpos (`VARCHAR(14)`).
   - Java: `com.fatorr.backend.util.CpfUtils` (`mask`, `unmask`, `isValid`)
   - TypeScript: `frontend/src/app/shared/utils/formatters.ts` (`maskCpf`, `unmaskCpf`, `isValidCpf`)

---

## 🚀 STATUS DAS 5 TRILHAS DE DESENVOLVIMENTO (100% IMPLEMENTADAS)

---

### 🔹 TRILHA 1: Autenticação, Perfis (RBAC) e Multi-Empresas
- **Status**: ✅ **100% Concluída**
- **Arquivos Backend**:
  - `backend/src/main/java/com/fatorr/backend/util/JwtUtils.java`
  - `backend/src/main/java/com/fatorr/backend/service/AuthService.java`
  - `backend/src/main/java/com/fatorr/backend/controller/AuthController.java`
  - `backend/src/main/java/com/fatorr/backend/controller/UsuarioController.java`
  - `backend/src/test/java/com/fatorr/backend/service/AuthServiceTest.java`
- **Arquivos Frontend**:
  - `frontend/src/app/services/auth.interceptor.ts`
  - `frontend/src/app/services/auth.service.ts`
  - `frontend/src/app/services/usuario.service.ts`
  - `frontend/src/app/pages/login/` (Entrar, Cadastro por Perfil, Recuperação de Senha)
  - `frontend/src/app/pages/usuarios-admin/` (Pré-cadastro por CPF, Gestão de Vínculos Pendentes)
- **Tarefas Realizadas**:
  - [x] Login com JWT (HMAC-SHA256), Refresh Token, BCryptPasswordEncoder com fallback seguro e HTTP Interceptor.
  - [x] Recuperação e Redefinição de Senha com token de 1h de expiração.
  - [x] Cadastro com seleção de perfil: Contador (com CRC e Organização), Empresário (múltiplos CNPJs) e Colaborador.
  - [x] Painel do Contabilista: pré-cadastro por CPF + Função e aprovação/rejeição de vínculos pendentes.
  - [x] Contexto global para troca de empresa ativa (`EmpresaContextService`).

---

### 🔹 TRILHA 2: Contas a Receber e Importação de XML de Notas Fiscais
- **Status**: ✅ **100% Concluída**
- **Arquivos Backend**:
  - `backend/src/main/java/com/fatorr/backend/controller/ContaReceberController.java`
  - `backend/src/main/java/com/fatorr/backend/service/ContaReceberService.java`
  - `backend/src/main/java/com/fatorr/backend/service/LeitorXmlNfeService.java`
  - `backend/src/test/java/com/fatorr/backend/service/LeitorXmlNfeServiceTest.java`
- **Arquivos Frontend**:
  - `frontend/src/app/services/conta-receber.service.ts`
  - `frontend/src/app/pages/contas-a-receber/`
- **Tarefas Realizadas**:
  - [x] CRUD completo de Contas a Receber com paginação e filtros por período, status e cliente.
  - [x] Baixa Manual de títulos (status `PAGO`, data de pagamento) e cancelamento.
  - [x] Parser seguro de XML de NF-e e NFS-e contra injeção XXE.
  - [x] Validação de emitente pertencente à empresa ativa logada e deduplicação por chave de acesso.
  - [x] Auto-cadastro do tomador em `clientes_fornecedores` com CNPJ alfanumérico.
  - [x] Suporte a múltiplas parcelas (`<dup>`).
  - [x] Interface completa com badges de status e Modal de Upload de XML com **Pré-visualização antes de salvar**.

---

### 🔹 TRILHA 3: Extrato Bancário e Conciliação Manual
- **Status**: ✅ **100% Concluída**
- **Arquivos Backend**:
  - `backend/src/main/java/com/fatorr/backend/controller/BankingController.java`
  - `backend/src/main/java/com/fatorr/backend/service/BankingService.java`
  - `backend/src/test/java/com/fatorr/backend/service/BankingServiceTest.java`
  - `seed_contas_receber.js` (corrigido para `clientes_fornecedores`)
- **Arquivos Frontend**:
  - `frontend/src/app/services/banking.service.ts`
  - `frontend/src/app/pages/banking/`
- **Tarefas Realizadas**:
  - [x] Parser de arquivos **OFX** e **CSV** com deduplicação por `FITID` e FITID sintético.
  - [x] Conciliação manual atômica (`@Transactional`) com validação estrita de multi-tenancy.
  - [x] Histórico de conciliações com detalhes dos lançamentos e títulos vinculados.
  - [x] Ação de **Desconciliação** em 1 clique (estorno com restauração de status).
  - [x] Interface Split-View com **Sugestões Inteligentes de Pareamento** por correspondência de valor.

---

### 🔹 TRILHA 4: Gestão Avançada e Importação de Folha de Pagamento (eSocial / ERP Contábil)
- **Status**: ✅ **100% Concluída**
- **Arquivos Backend**:
  - `backend/src/main/java/com/fatorr/backend/controller/PayrollController.java`
  - `backend/src/main/java/com/fatorr/backend/service/PayrollService.java`
  - `backend/src/main/java/com/fatorr/backend/dto/ImportFolhaResponseDTO.java`
  - `backend/src/main/java/com/fatorr/backend/dto/ProLaboreIdealDTO.java`
  - `backend/src/test/java/com/fatorr/backend/service/PayrollServiceTest.java`
- **Arquivos Frontend**:
  - `frontend/src/app/services/payroll.service.ts`
  - `frontend/src/app/pages/payroll/`
- **Tarefas Realizadas**:
  - [x] Modelagem e CRUD de Folha de Pagamento Detalhada (`folhas_detalhadas`).
  - [x] **Sincronização Atômica com Fator R**: atualização imediata da tabela `folhas_pagamento` da competência.
  - [x] Parser XML do eSocial eventos **S-1200** e **S-1210** com discriminação entre salários CLT e pró-labore de sócios.
  - [x] Importador de planilhas CSV e Excel (`.xlsx` via Apache POI) de ERPs contábeis (Domínio, Alterdata, Fortes, Questor).
  - [x] **Calculadora de Pró-Labore Mínimo Ideal**: cálculo do pró-labore exato para atingir 28% (Anexo III) ponderando INSS sócio (11% com teto) e tabela progressiva de IRPF vs. economia no Simples Nacional.
  - [x] Dashboard analítico da folha com simulador interativo de Pró-Labore Ideal.

---

### 🔹 TRILHA 5: Dossiê Tributário, Emissão de Parecer Técnico (PDF/Excel) e Auditoria Fiscal
- **Status**: ✅ **100% Concluída**
- **Arquivos Backend**:
  - `backend/src/main/java/com/fatorr/backend/controller/ReportsController.java`
  - `backend/src/main/java/com/fatorr/backend/service/ReportsService.java`
  - `backend/src/main/java/com/fatorr/backend/dto/ParametrosEmissaoLaudoDTO.java`
  - `backend/src/main/java/com/fatorr/backend/dto/ValidacaoLaudoResponseDTO.java`
  - `backend/src/test/java/com/fatorr/backend/service/ReportsServiceTest.java`
- **Arquivos Frontend**:
  - `frontend/src/app/services/reports.service.ts`
  - `frontend/src/app/pages/reports/`
- **Tarefas Realizadas**:
  - [x] Motor de geração de **Laudos em PDF timbrados (`OpenPDF`)** com identificação da contabilidade (CRC do contador) e empresa cliente.
  - [x] Memória de cálculo analítica de 12 meses ($M-12$ a $M-1$).
  - [x] Estudo comparativo Simples Nacional vs. Lucro Presumido com economia estimada.
  - [x] Projeções futuras para $M+1$ e $M+2$ nos cenários Otimista, Neutro e Pessimista.
  - [x] Exportação analítica em **Planilha Excel (`.xlsx` via Apache POI)** com abas estilizadas e fórmulas dinâmicas (`SUM`).
  - [x] Certificado de Autenticidade com **Hash SHA-256** gravado em `laudos_tributarios`.
  - [x] Central de emissão com pré-visualizador de PDF no navegador, histórico e validador público de laudos por hash.
