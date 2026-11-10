# Roteiro de Testes do Sistema Fator R

- Página Inicial (`/login`)
  - Aba Entrar
    - Testar login com CPF
    - Testar login com E-mail
    - Testar link "Esqueceu a senha?"
  - Aba Cadastrar
    - Cadastrar Contador
    - Cadastrar Empresário
    - Cadastrar Colaborador
  - Aba Recuperar Senha
    - Gerar token de recuperação
    - Redefinir senha com token

- Barra Superior e Menu Lateral (Layout Global)
  - Testar botão de recolher/expandir menu lateral
  - Selecionar empresa ativa no seletor global do topo
  - Testar botão de encerrar sessão (Logout)

- Página Empresas (`/empresas`)
  - Testar botão "+ Nova Empresa" e salvar cadastro
  - Testar botão "Gerenciar Acessos" da empresa
    - Vincular novo acesso por CPF e senha
    - Desvincular usuário
  - Testar botão "Excluir" empresa

- Página Usuários & Acessos (`/usuarios-admin`)
  - Pré-cadastrar colaborador por CPF, função e empresa
  - Aprovar solicitação de vínculo pendente
  - Rejeitar solicitação de vínculo pendente
  - Verificar listagem de usuários e empresas vinculadas

- Página Parâmetros Globais (`/parametros-padrao`)
  - Editar alíquotas padrão do Lucro Presumido
  - Editar/adicionar/remover faixas do Anexo III e Anexo V
  - Testar botão de salvar parâmetros padrão

- Página Parâmetros Empresa (`/parametros`)
  - Alterar alíquotas do Lucro Presumido da empresa ativa
  - Alterar/adicionar/remover faixas do Anexo III e Anexo V
  - Testar botão "Salvar Específico"

- Página Contas a Receber (`/contas-a-receber`)
  - Testar botão "Importar XML NFS-e / NF-e"
    - Testar importação de XML NF-e à vista (`arquivos para testes/faturamento/...`)
    - Testar importação de XML NFS-e (`arquivos para testes/faturamento/...`)
    - Testar importação de XML NF-e parcelada (`arquivos para testes/faturamento/...`)
    - Marcar opção "Atualizar/alimentar o Faturamento Mensal" e confirmar
  - Testar filtros (Status, Cliente/Fornecedor, Data de Vencimento) e botão "Limpar"
  - Testar botão "Baixar" (baixa manual como PAGO)
  - Testar botão "Cancelar" (cancelar título)
  - Testar botão "NF Cancelada" (cancelar nota e abater do faturamento)
  - Testar botões de paginação ("Anterior" e "Próxima")

- Página Conciliação Bancária (`/banking`)
  - Testar botão "Importar OFX" (`arquivos para testes/extrato bancario/...`)
  - Testar botão "Importar CSV" (`arquivos para testes/extrato bancario/...`)
  - Testar filtro por Mês / Competência e botão "Limpar Filtro"
  - Aba Conciliação Pendente
    - Selecionar lançamento do extrato à esquerda e conta a receber à direita
    - Verificar destaque de sugestão de match
    - Preencher observação e testar botão "Conciliar Selecionados"
    - Testar botão "Excluir" em lançamento pendente do extrato
  - Aba Histórico de Conciliações
    - Testar botão "Atualizar Histórico"
    - Testar botão "Desconciliar"

- Página Folha & eSocial (`/payroll`)
  - Aba Importador eSocial & ERP
    - Testar importação de XML do eSocial S-1200 (`arquivos para testes/folha de pagamento/...`)
    - Testar importação de Planilha CSV mensal (`arquivos para testes/folha de pagamento/...`)
    - Testar importação de Planilha CSV anual em lote (`folha_anual_2026.csv`)
    - Testar botão "Simular Pró-Labore com Esta Base"
  - Aba Folhas Detalhadas
    - Verificar cards de totais (Salários CLT, Pró-Labore, Encargos, Folha Total)
    - Testar botão "Atualizar"
    - Testar botão de excluir folha na tabela
  - Aba Simulador de Pró-Labore Ideal
    - Selecionar Competência de Cálculo
    - Ajustar Faturamento Mensal Previsto (barra deslizante e botões rápidos)
    - Verificar cálculo de Pró-Labore Mínimo Ideal, INSS, IRPF e Economia no Simples
  - Aba Lançamento Manual
    - Preencher competência, salários, pró-labore, 13º, INSS e FGTS
    - Testar botão "Salvar e Sincronizar Fator R"

- Página Simulador Fator R (`/simulador`)
  - Selecionar Mês e Ano de referência (e testar botão "Mês Atual")
  - Testar botão "Importar XML NF-e" direto pelo simulador
  - Testar botão "+ Inserir Dados para Simulação" (salvar faturamento e folha manual)
  - Verificar medidor (Gauge) do Fator R e enquadramento (Anexo III ou Anexo V)
  - Verificar Comparativo de Regimes (Simples Nacional vs Lucro Presumido)
  - Tabela Histórico Base (Últimos 12 Meses)
    - Testar botão "Encerrar Mês"
    - Testar bloqueio de importação/edição em mês encerrado
    - Testar botão "Reabrir" mês
  - Verificar cards de Projeções Futuras (Cenários Otimista, Neutro e Pessimista)

- Página Dossiês & Laudos (`/reports`)
  - Aba Central de Emissão
    - Selecionar Ano, Mês e Tipo de Parecer / Documento
    - Marcar/desmarcar opções de Projeção de Cenários e Assinatura do Contador
    - Preencher parecer técnico personalizado
    - Testar botão "Gerar e Visualizar Laudo PDF" (pré-visualização em tela)
    - Testar botão "Baixar PDF (.pdf)"
    - Testar botão "Baixar Planilha Excel (.xlsx)"
  - Aba Histórico de Laudos
    - Testar botão "Atualizar Lista"
    - Testar botão "Baixar Laudo PDF" de um item do histórico
    - Testar botão "Copiar Hash e Validar"
  - Aba Auditoria Fiscal & Hash SHA-256
    - Colar chave Hash SHA-256 e testar botão "Verificar Autenticidade"
    - Testar validação com um Hash inválido/alterado

- Página Home / Dashboard (`/home`)
  - Verificar atualização dos 4 cards de KPIs (Fator R Atual, Economia Mensal, Contas a Receber, Conciliação & Laudos)
  - Verificar painel Janela Deslizante (RBT12, Folha 12M e Alíquota Efetiva)
  - Verificar tabela de Próximos Títulos a Receber
  - Testar botões de atalho rápido para os módulos do sistema
