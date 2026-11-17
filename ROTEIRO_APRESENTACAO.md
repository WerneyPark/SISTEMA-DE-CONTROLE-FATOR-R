# 🚀 Roteiro de Apresentação: Evolução do Projeto (Pós-06/10)
## Entregas, Novos Módulos e Demonstração do Protótipo Funcional

> **Foco da Apresentação**: Demonstrar as evoluções implementadas desde a última apresentação (06/10), com base nos commits do repositório.  
> **Formato**: 3 Apresentadores  
> **Duração Estimada**: 10 a 12 minutos (aprox. 3 a 4 minutos por integrante)  
> **Base de Commits**: `e3c66c9`, `213a3c1`, `95f3efa`, `deacd55`, `cb3fbf4` e `1ba889d`.

---

## 👥 Divisão de Papéis e Pauta das Mudanças (Pós-06/10)

| Apresentador | Foco das Mudanças desde 06/10 | Commits de Referência | Telas da Demonstração |
| :--- | :--- | :---: | :--- |
| **Apresentador 1** | • Saneamento e integração Fullstack (Java 21 + Angular 21)<br>• Gestão Multi-Empresa e Acessos<br>• Novo Módulo: Contas a Receber & Importador Seguro NF-e/NFS-e com Preview | `e3c66c9`<br>`95f3efa` | • Layout / Topo (`Empresas`)<br>• Contas a Receber (`/contas-a-receber`) |
| **Apresentador 2** | • Novo Módulo: Banking & Conciliação OFX/CSV com FITID<br>• Interface Split-View com Match Automático e Desconciliação<br>• Novo Recurso: Encerramento de Mês / Fechamento Fiscal | `e3c66c9`<br>`213a3c1`<br>`cb3fbf4` | • Conciliação (`/banking`)<br>• Simulador (`/simulador`) |
| **Apresentador 3** | • Novo Módulo: Folha Analítica e Leitor do eSocial (**S-1200**)<br>• Algoritmo do Pró-Labore Ideal (INSS + IRPF vs Simples)<br>• Novo Módulo: Dossiê Tributário com Laudo PDF, Excel e Hash SHA-256 | `e3c66c9`<br>`deacd55` | • Folha (`/payroll`)<br>• Laudos & Auditoria (`/reports`) |

---

## ⚡ Setup Rápido para a Apresentação (30 segundos)

Certifique-se de que os serviços estão rodando antes de começar:
```bash
# 1. Banco de Dados
docker compose up -d

# 2. Backend (Terminal 1)
cd backend && ./mvnw spring-boot:run

# 3. Frontend (Terminal 2)
cd frontend && nvm use 20 && npm start
```
*Acesse no navegador: **`http://localhost:4200`** com o usuário Contador: CPF `11122233344` | Senha: `123456`.*

---

# 🎬 Roteiro Fala por Fala & Passos ao Vivo

---

## 👤 APRESENTADOR 1: Integração Fullstack, Multi-Empresa e Contas a Receber
**Tempo**: `00:00` a `03:30` (3.5 minutos)  
**Commits**: `e3c66c9` *(Saneamento arquitetural e fullstack)* e `95f3efa` *(Ajustes Contas a Receber e Empresas)*

### 🎙️ Fala do Apresentador 1:

> *"Olá a todos. Na nossa última apresentação em 06/10, tínhamos demonstrado o protótipo conceitual e a estrutura inicial de telas.  
> Desde o dia 06/10, o projeto passou por uma evolução profunda: eliminamos qualquer estrutura mockada e entregamos a **integração fullstack definitiva**, unindo a API em **Spring Boot 4 (Java 21)**, o banco relacional **PostgreSQL 16** e a SPA em **Angular 21.2**.
>
> *As primeiras grandes mudanças pós-06/10 foram:*
>
> 1. ***Gestão Multi-Empresa e Controle de Vínculos:*** Agora o sistema permite cadastrar e alternar entre múltiplas empresas clientes no topo da aplicação, com isolamento transacional de dados e aprovação de usuários por CPF.
> 2. ***Módulo Completo de Contas a Receber:*** Criamos a gestão financeira de títulos com filtros avançados e, principalmente, o **Importador Seguro de Notas Fiscais (NF-e, NFS-e e notas parceladas)**.
> 3. ***Segurança e UX no XML:*** O backend foi blindado contra vulnerabilidades do tipo XXE (XML External Entity), auto-cadastra os clientes tomadores da nota e abre uma **tela de pré-visualização** das duplicatas antes de gravar no banco.
>
> *Vamos ver isso funcionando ao vivo."*

### 🖥️ O que mostrar na tela (Apresentador 1):
1. **No topo do sistema (`Header`)**:
   - Mostrar o seletor de empresa ativa: alternar entre *Empresa Alpha* e *Empresa Beta*, mostrando que os dados mudam em tempo real.
2. **Entrar em Contas a Receber (`/contas-a-receber`)**:
   - Clicar no botão **Importar XML NFS-e / NF-e**.
   - Fazer upload de um arquivo de teste de nota parcelada:  
     `arquivos para testes/faturamento/01 - Inovar Softwares/2026/02/nfe_parcelada_2026_02_26063.xml`
   - **Destacar**: O modal de pré-visualização abre na hora com as parcelas identificadas.
   - Clicar em **Confirmar**: os títulos entram na lista como `PENDENTE`.
   - Clicar em **Baixar** em um dos títulos: ele vira `PAGO` imediatamente.

👉 **Passagem de bastão**:  
> *"Agora que o faturamento das notas fiscais foi integrado ao sistema, o **[Nome do Apresentador 2]** vai mostrar as novas implementações de conciliação bancária e a nova trava de fechamento fiscal."*

---

## 👤 APRESENTADOR 2: Conciliação Bancária OFX/CSV e Encerramento de Mês
**Tempo**: `03:30` a `07:00` (3.5 minutos)  
**Commits**: `e3c66c9` *(Módulo Banking)*, `213a3c1` *(Extratos reais)* e `cb3fbf4` *(Ajustes de Encerramento de Faturamento)*

### 🎙️ Fala do Apresentador 2:

> *"Obrigado, [Nome do Apresentador 1].  
> Uma das principais entregas que fizemos após o dia 06/10 foi o módulo de **Banking e Conciliação Bancária**, que valida se a receita faturada realmente caiu na conta bancária da empresa.
>
> *O que foi implementado neste período:*
>
> 1. ***Parser Oficial de OFX e CSV:*** O sistema agora lê extratos bancários reais emitidos por qualquer banco.
> 2. ***Deduplicação por FITID:*** Evitamos lançamentos duplicados gravando o código identificador da transação bancária.
> 3. ***Interface Split-View com Match Automático:*** Desenvolvemos uma visualização em duas colunas: extrato bancário à esquerda e contas a receber à direita. O sistema detecta valores idênticos e datas próximas e destaca a sugestão de correspondência para o contador conciliar em 1 clique.
> 4. ***Desconciliação Atômica:*** Se houver qualquer erro operacional, é possível reverter o lançamento no histórico restaurando o título para pendente.
> 5. ***Novo Recurso de Encerramento de Mês (Commit cb3fbf4):*** Implementamos a trava de competência. Quando o contador fecha o mês, a competência é bloqueada, impedindo que novas notas ou edições alterem o Fator R de um período já declarado à Receita Federal.
>
> *Vamos ver a conciliação e a trava funcionando na prática."*

### 🖥️ O que mostrar na tela (Apresentador 2):
1. **Entrar em Conciliação Bancária (`/banking`)**:
   - Clicar em **Importar OFX** e carregar o extrato:  
     `arquivos para testes/extrato bancario/01 - Inovar Softwares/2026/02/extrato_2026_02.ofx`
   - Mostrar a coluna da esquerda com os lançamentos do banco.
2. **Realizar o Match**:
   - Clicar em um lançamento de crédito e selecionar o título correspondente à direita.
   - Clicar em **Conciliar Selecionados**: o lançamento recebe o badge verde `CONCILIADO`.
   - Ir na aba **Histórico de Conciliações** e mostrar o botão **Desconciliar** com estorno imediato.
3. **Mostrar a Trava de Encerramento (`/simulador`)**:
   - Ir no menu **Simulador Fator R**.
   - Na tabela de histórico, mostrar o novo botão **"Encerrar Mês"**.
   - Clicar em encerrar na competência: mostrar o status de fechado e explicar que o sistema trava alterações acidentais naquele período contábil.

👉 **Passagem de bastão**:  
> *"Com as receitas auditadas e o mês fechado, o **[Nome do Apresentador 3]** vai demonstrar as inovações na folha de pagamento com eSocial, o simulador de pró-labore ideal e os novos laudos técnicos com assinatura digital."*

---

## 👤 APRESENTADOR 3: Folha com eSocial, Pró-Labore Ideal e Laudos SHA-256
**Tempo**: `07:00` a `10:30` (3.5 minutos)  
**Commits**: `e3c66c9` *(Folha e Relatórios)* e `deacd55` *(Ajustes de Relatórios e Pró-Labore Ideal)*

### 🎙️ Fala do Apresentador 3:

> *"Obrigado, [Nome do Apresentador 2].  
> Para fechar a fórmula do Fator R, precisávamos da outra ponta: a **Folha de Pagamento**.
>
> *As principais novidades implementadas pós-06/10 foram:*
>
> 1. ***Integração com eSocial Oficial:*** Desenvolvemos o parser dos arquivos XML dos eventos **S-1200** (Remuneração) e **S-1210** (Pagamentos), além de suporte a planilhas anuais em lote geradas por ERPs contábeis como Domínio e Alterdata. Ao importar, o Fator R é recalculado no mesmo instante.
> 2. ***Calculadora do Pró-Labore Mínimo Ideal (Commit deacd55):*** Esta foi uma das maiores inteligências fiscais adicionadas. O sistema não apenas diz qual é o Fator R, mas calcula o valor cirúrgico de pró-labore necessário para atingir os **28% do Anexo III**, ponderando o custo de INSS (11% com teto) e a tabela progressiva do IRPF do sócio versus a economia gerada no Simples Nacional.
> 3. ***Dossiê Tributário com Laudo PDF e Excel:*** Desenvolvemos o motor de relatórios em **OpenPDF** e **Apache POI**, gerando laudos timbrados com o número do **CRC do contador**, memória de cálculo analítica e projeções para os meses seguintes.
> 4. ***Auditoria e Integridade por Hash SHA-256:*** Cada laudo gerado recebe uma chave criptográfica SHA-256 gravada no banco, com tela de validação pública para comprovar que o documento não foi adulterado.
>
> *Vamos ver a folha, o simulador e o laudo em ação."*

### 🖥️ O que mostrar na tela (Apresentador 3):
1. **Entrar em Folha de Pagamento (`/payroll`)**:
   - Clicar em **Importar eSocial (XML)** e selecionar:  
     `arquivos para testes/folha de pagamento/01 - Inovar Softwares/2026/02/esocial_s1200_2026_02.xml`
   - Mostrar a folha importada discriminando salários e pró-labore.
2. **Mostrar a Calculadora de Pró-Labore Ideal**:
   - Ir na aba **Simulador de Pró-Labore Ideal**.
   - Mostrar o card que calcula automaticamente:
     - Quanto de pró-labore falta para atingir 28%;
     - O desconto de INSS e IRPF do sócio;
     - O indicativo se o ajuste compensa financeiramente perante a economia do Simples Nacional.
3. **Entrar em Dossiês & Laudos (`/reports`)**:
   - Selecionar a competência e clicar em **Gerar e Visualizar Laudo PDF**.
   - Mostrar o laudo timbrado na tela com o **CRC do contador** e o código **Hash SHA-256** no rodapé.
   - Copiar o Hash, colar na aba **Auditoria Fiscal** e clicar em **Verificar Autenticidade**: exibir o selo de documento íntegro e autêntico.

---

## 👥 ENCERRAMENTO CONJUNTO (1 Minuto)
**Apresentador 3 convida todos para a fala final**:

> **Apresentador 1**: *"Em resumo, desde a apresentação de 06/10, o projeto evoluiu de um esboço inicial para uma plataforma corporativa completa, 100% integrada e testada ponta a ponta com notas, extratos e eventos do eSocial reais."*  
> **Apresentador 2**: *"Garantimos segurança de ponta a ponta, conciliação com extratos bancários e travas operacionais de fechamento contábil."*  
> **Apresentador 3**: *"E entregamos inteligência fiscal com cálculo de pró-labore e emissão de laudos oficiais auditados por SHA-256. Muito obrigado a todos e estamos à disposição para dúvidas!"*

---

## 📌 Guia de Arquivos para Utilizar na Demonstração

Para não perder tempo procurando arquivos durante a apresentação, utilize esta pasta:

| Módulo | Arquivo de Teste Recomendado |
| :--- | :--- |
| **Nota Fiscal (NF-e)** | `arquivos para testes/faturamento/01 - Inovar Softwares/2026/02/nfe_parcelada_2026_02_26063.xml` |
| **Extrato Bancário (OFX)** | `arquivos para testes/extrato bancario/01 - Inovar Softwares/2026/02/extrato_2026_02.ofx` |
| **Folha eSocial (XML)** | `arquivos para testes/folha de pagamento/01 - Inovar Softwares/2026/02/esocial_s1200_2026_02.xml` |
