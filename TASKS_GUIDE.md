# 📖 GUIA DE DESENVOLVIMENTO E DIVISÃO DE TAREFAS

Este documento contém o roadmap, as regras de arquitetura e as instruções para os desenvolvedores que atuarão nas novas funcionalidades do sistema.

---

## 🏛️ REGRAS GERAIS DE ARQUITETURA (OBRIGATÓRIO PARA TODOS)

1. **CNPJ Alfanumérico (Padrão RFB)**:
   - Todo campo de CNPJ no banco, backend e frontend é do tipo `String / VARCHAR(20)`.
   - Utilize sempre as funções de `src/shared/utils/cnpj.ts`:
     - `maskCnpj(valor)`: Aplica a máscara `XX.XXX.XXX/XXXX-XX` (suporta letras e números).
     - `unmaskCnpj(valor)`: Remove caracteres especiais e retorna string limpa em caixa alta.
     - `isValidCnpj(valor)`: Valida se possui 14 caracteres alfanuméricos válidos.

2. **Isolamento de Código (Evite conflitos de Merge)**:
   - Cada trilha deve trabalhar **exclusivamente** dentro da pasta do seu respectivo módulo em `src/modules/` e no frontend em `src/app/pages/<modulo>/`.
   - Não altere as pastas de outros módulos nem os tipos globais sem alinhamento prévio.

3. **Tipos e Contratos**:
   - Utilize os tipos compartilhados definidos em `src/shared/types/` (`auth.types.ts`, `receivables.types.ts`, `banking.types.ts`, `payroll.types.ts`, `reports.types.ts`, `common.types.ts`).

---

## 🚀 TRILHAS DE DESENVOLVIMENTO PARALELO (5 DESENVOLVEDORES)

---

### 🔹 TRILHA 1: Autenticação, Perfis (RBAC) e Multi-Empresas
- **Branch**: `feature/auth-rbac-empresas`
- **Pasta de Trabalho**: `src/modules/auth/` e `frontend/src/app/pages/auth/`
- **Tarefas Principais**:
  - [ ] Implementar fluxo de Login com JWT e Refresh Token no backend e interceptor no frontend.
  - [ ] Implementar fluxo de Recuperação de Senha (e-mail + token com tempo de expiração).
  - [ ] Cadastro com seleção de perfil:
    - **Contador**: Obriga informar CRC e Organização Contábil (`organizacoes_contabeis`).
    - **Empresário**: Permite vincular a múltiplos CNPJs alfanuméricos.
    - **Usuário Colaborador**: Informa o CNPJ da empresa (status fica `PENDENTE`).
  - [ ] Painel do Contabilista (Admin):
    - Cadastro inicial de empresas vinculadas à sua contabilidade.
    - Aprovação / Rejeição de solicitações de vínculos de usuários.
    - Pré-cadastro de usuário informando apenas **CPF** e **Função** (quando o usuário se registrar com o CPF, é ativado e vinculado automaticamente).
  - [ ] Endpoint/Contexto para troca de empresa ativa (`empresaIdAtiva`).

---

### 🔹 TRILHA 2: Contas a Receber e Importação de XML de Notas Fiscais
- **Branch**: `feature/contas-a-receber-xml`
- **Pasta de Trabalho**: `src/modules/receivables/` e `frontend/src/app/pages/receivables/`
- **Tarefas Principais**:
  - [ ] CRUD de Contas a Receber com paginação e filtros (por período, status e cliente).
  - [ ] Ação de Baixa Manual de título (muda status para `PAGO` e grava data de pagamento).
  - [ ] Parser de XML de Notas Fiscais (NFS-e e NF-e padrão nacional):
    - Extrair dados do Prestador (Emitente), Tomador (Cliente), Valor, Data e Parcelas.
    - Validar se o Emitente é a empresa ativa logada.
    - **Auto-cadastro**: Se o Tomador não existir no banco da empresa, cadastrar automaticamente em `clientes_fornecedores` com CNPJ alfanumérico.
    - Gerar o lançamento em `contas_a_receber` com `origem = 'IMPORTACAO_XML'`.
  - [ ] Frontend: Listagem de títulos com badges de status e Modal de Upload de XML com pré-visualização antes de salvar.

---

### 🔹 TRILHA 3: Extrato Bancário e Conciliação Manual
- **Branch**: `feature/extrato-conciliacao-manual`
- **Pasta de Trabalho**: `src/modules/banking/` e `frontend/src/app/pages/banking/`
- **Tarefas Principais**:
  - [ ] Parser de arquivos de extrato **OFX** e **CSV**:
    - Extrair Data, Descrição, Valor, Tipo (Débito/Crédito) e `FITID`.
    - Salvar em `extrato_lancamentos` ignorando transações duplicadas (mesmo `fitid` para a mesma empresa).
  - [ ] Endpoint de Conciliação Manual em transação atômica:
    - Recebe `extratoLancamentoId` e `contaReceberId`.
    - Atualiza o extrato para `CONCILIADO`, o título para `PAGO` e grava registro na tabela `conciliacoes_bancarias`.
  - [ ] Frontend: Tela de Conciliação em Split-View (Lado esquerdo: Lançamentos bancários; Lado direito: Títulos a receber) permitindo seleção e vinculação manual com 1 clique.

---

### 🔹 TRILHA 4: Gestão Avançada e Importação de Folha de Pagamento (eSocial / ERP Contábil)
- **Branch**: `feature/folha-pagamento-esocial`
- **Pasta de Trabalho**: `src/modules/payroll/` e `frontend/src/app/pages/payroll/`
- **Tarefas Principais**:
  - [ ] Modelagem e CRUD de Folha de Pagamento Detalhada (`folhas_detalhadas`):
    - Discriminação de Salários/Ordenados, Pró-Labore de Sócios, 13º Salário, INSS Patronal e FGTS.
  - [ ] Parser de arquivos de Folha de Pagamento:
    - **eSocial XML**: Extração dos eventos `S-1200` (Remuneração) e `S-1210` (Pagamentos).
    - **CSV/Excel Analítico**: Importador para relatórios exportados por ERPs contábeis (Domínio Sistemas, Alterdata, Fortes, Questor).
  - [ ] Atualização automática e atômica da tabela `folhas_pagamento` da competência para alimentar o cálculo do Fator R.
  - [ ] **Calculadora de Pró-Labore Mínimo Ideal**:
    - Endpoint que calcula o valor exato de pró-labore que a empresa precisa retirar para atingir a margem de 28% (Anexo III) com a menor carga tributária combinada (INSS + IRPF).
  - [ ] Frontend: Dashboard da folha de pagamento, upload de arquivos com preview analítico e simulador interativo de pró-labore ideal.

---

### 🔹 TRILHA 5: Dossiê Tributário, Emissão de Parecer Técnico (PDF/Excel) e Auditoria Fiscal
- **Branch**: `feature/dossie-relatorios-tributarios`
- **Pasta de Trabalho**: `src/modules/reports/` e `frontend/src/app/pages/reports/`
- **Tarefas Principais**:
  - [ ] Motor de Geração de Documentos em PDF (OpenPDF / Thymeleaf-PDF):
    - **Laudo Executivo de Enquadramento do Fator R**: Documento formal timbrado com identificação da contabilidade (CRC do contador) e da empresa cliente.
    - **Memória de Cálculo de 12 Meses**: Discriminação analítica de todas as receitas e folhas da janela deslizante.
    - **Estudo Comparativo Simples vs Lucro Presumido**: Tabela comparativa e economia estimada anualizada.
    - **Parecer Conclusivo e Projeções Futuras**: Parecer recomendatório para os cenários Otimista, Neutro e Pessimista.
  - [ ] Exportação Analítica em Planilha Excel (`.xlsx` via Apache POI):
    - Tabela auditável com fórmulas dinâmicas para conferência de contadores e peritos tributários.
  - [ ] Registro e Autenticidade de Laudos (`laudos_tributarios`):
    - Armazenamento de metadados, usuário emissor e código hash SHA-256 para verificação de autenticidade documental.
  - [ ] Frontend: Central de Emissão de Dossiês com opções de personalização, pré-visualizador de PDF no navegador e histórico de laudos gerados.

---

## 🌿 FLUXO DE GIT PARA CADA DESENVOLVEDOR

1. **Iniciar a tarefa**:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/nome-da-sua-trilha
   ```

2. **Commit e Boas Práticas**:
   - Faça commits atômicos utilizando o padrão Conventional Commits (ex: `feat(payroll): adiciona parser para esocial s-1200`, `feat(reports): cria motor de geracao de laudo em pdf`).

3. **Submeter para Revisão**:
   ```bash
   git push -u origin feature/nome-da-sua-trilha
   ```
   - Abra um Pull Request direcionado à branch principal do projeto.
