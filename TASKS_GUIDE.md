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
   - Cada trilha deve trabalhar **exclusivamente** dentro da pasta do seu respectivo módulo em `src/modules/`.
   - Não altere as pastas de outros módulos nem os tipos globais sem alinhamento prévio.

3. **Tipos e Contratos**:
   - Utilize os tipos compartilhados definidos em `src/shared/types/` (`auth.types.ts`, `receivables.types.ts`, `banking.types.ts`).

---

## 🚀 TRILHAS DE DESENVOLVIMENTO PARALELO

---

### 🔹 TRILHA 1: Autenticação, Perfis (RBAC) e Multi-Empresas
- **Branch**: `feature/auth-rbac-empresas`
- **Pasta de Trabalho**: `src/modules/auth/` e `src/pages/auth/` (ou equivalente no frontend)
- **Tarefas Principais**:
  - [ ] Implementar fluxo de Login com JWT e Refresh Token.
  - [ ] Implementar fluxo de Recuperação de Senha (e-mail + token de expiração).
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
- **Pasta de Trabalho**: `src/modules/receivables/` e `src/pages/receivables/`
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
- **Pasta de Trabalho**: `src/modules/banking/` e `src/pages/banking/`
- **Tarefas Principais**:
  - [ ] Parser de arquivos de extrato **OFX** e **CSV**:
    - Extrair Data, Descrição, Valor, Tipo (Débito/Crédito) e `FITID`.
    - Salvar em `extrato_lancamentos` ignorando transações duplicadas (mesmo `fitid` para a mesma empresa).
  - [ ] Endpoint de Conciliação Manual em transação atômica:
    - Recebe `extratoLancamentoId` e `contaReceberId`.
    - Atualiza o extrato para `CONCILIADO`, o título para `PAGO` e grava registro na tabela `conciliacoes_bancarias`.
  - [ ] Frontend: Tela de Conciliação em Split-View (Lado esquerdo: Lançamentos bancários; Lado direito: Títulos a receber) permitindo seleção e vinculação manual com 1 clique.

---

## 🌿 FLUXO DE GIT PARA CADA DESENVOLVEDOR

1. **Iniciar a tarefa**:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/nome-da-sua-trilha
   ```

2. **Commit e Boas Práticas**:
   - Faça commits atômicos utilizando o padrão Conventional Commits (ex: `feat(auth): adiciona fluxo de login com jwt`, `fix(receivables): corrige parser de data de nfe`).

3. **Submeter para Revisão**:
   ```bash
   git push -u origin feature/nome-da-sua-trilha
   ```
   - Abra um Pull Request direcionado à branch `main`.
