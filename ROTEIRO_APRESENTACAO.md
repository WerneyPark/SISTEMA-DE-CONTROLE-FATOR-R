# 🎤 Roteiro Oficial de Apresentação: Sistema de Controle Fator R
## Apresentação do Produto Final + Demonstração do Protótipo Funcional

> **Público-Alvo**: Bancas Avaliadoras, Gestores de TI, Especialistas Fiscais e Contábeis  
> **Formato**: 3 Apresentadores  
> **Duração Estimada**: 15 a 20 minutos (aprox. 5 minutos por integrante + encerramento)  
> **Arquitetura em Demonstração**: Java 21 LTS, Spring Boot 4.0.6, Angular 21.2, PostgreSQL 16 Alpine, OpenPDF e Apache POI.

---

## 👥 Divisão de Papéis e Estrutura da Apresentação

| Integrante | Papel Principal | Foco Temático | Trilhas e Módulos Abordados | Tempo Estimado |
| :--- | :--- | :--- | :--- | :--- |
| **Apresentador 1** | **O Estrategista / Tech Lead** | Dor de Mercado, Fator R, Regras Fiscais e Arquitetura Base | Visão de Negócio, Regras da LC 123/06, CNPJ Alfanumérico, **Trilha 1** (Auth JWT, Perfis RBAC, Multi-Empresas). | **00:00 - 05:00** (5 min) |
| **Apresentador 2** | **O Engenheiro Financeiro & Integrações** | Automação de Receitas, Documentos Eletrônicos e Bancos | **Trilha 2** (Contas a Receber, Parser Seguro XML NF-e/NFS-e) e **Trilha 3** (Banking OFX/CSV, FITID e Conciliação Split-View). | **05:00 - 10:30** (5.5 min) |
| **Apresentador 3** | **O Auditor Fiscal & Especialista Tributário** | Inteligência de Folha, Pró-Labore Ideal e Laudos Técnicos | **Trilha 4** (Folha eSocial S-1200/S-1210, Otimizador Pró-Labore), **Trilha 5** (Laudo PDF com CRC, SHA-256 e Excel) e Conclusão. | **10:30 - 16:00** (5.5 min) |
| **Todos** | **Equipe Completa** | Encerramento Executivo e Perguntas & Respostas | Considerações finais e Sessão de Q&A com a banca/avaliação. | **16:00 - 20:00** (4 min) |

---

## 🛠️ Checklist Técnico Pré-Apresentação (15 Minutos Antes)

Antes de iniciar a apresentação na sala ou projetor:

- [ ] **1. Banco de Dados Ativo**:
  ```bash
  docker compose up -d
  # Confirmar que a porta 5432 está alocada para o fatorr_db
  ```
- [ ] **2. Backend Ativo**:
  ```bash
  cd backend
  ./mvnw spring-boot:run
  # Aguardar a mensagem: "Tomcat initialized with port 8080 (http)" e "Seeds executadas"
  ```
- [ ] **3. Frontend Ativo**:
  ```bash
  cd frontend
  nvm use 20
  npm start
  # Abrir http://localhost:4200 em tela cheia (F11)
  ```
- [ ] **4. Pasta de Testes Aberta no Navegador de Arquivos**:
  - Deixar a pasta `arquivos para testes/` aberta e visível para arrastar arquivos durante a apresentação (XMLs de NF-e, OFX de banco e XMLs do eSocial).
- [ ] **5. Credenciais de Demonstração em Mãos**:
  - **Contabilista**: CPF `11122233344` | Senha: `123456`
  - **Empresário**: CPF `22233344455` | Senha: `123456`

---

# 🎬 Roteiro Detalhado: Fala por Fala & Passos Práticos

---

## 👤 PARTE 1: Visão Estratégica, Negócio e Arquitetura Base
**Responsável**: Apresentador 1  
**Tempo**: `00:00` a `05:00` (5 minutos)  
**Telas Utilizadas**: Slide Inicial / Tela de Login (`/login`) / Dashboard Inicial (`/home`) / Gestão de Empresas e Usuários (`/usuarios-admin`)

### 🎯 Objetivos Desta Etapa:
1. Contextualizar a dor real de mercado das empresas de serviços no Simples Nacional.
2. Explicar matematicamente a fórmula do **Fator R** (LC 123/2006).
3. Apresentar a stack corporativa (Java 21, Spring Boot 4, Angular 21, PostgreSQL 16).
4. Demonstrar ao vivo a segurança (JWT, RBAC multi-perfil, CNPJ alfanumérico e troca de contexto de empresa).

---

### 🎙️ Roteiro de Fala Sugerido (Apresentador 1):

> *"Bom dia/Boa tarde a todos os presentes e membros da banca.  
> Hoje temos o orgulho de apresentar o **Sistema de Controle Fator R**, uma plataforma corporativa completa desenvolvida para solucionar uma das maiores dores do planejamento tributário brasileiro: o enquadramento fiscal de empresas prestadoras de serviços do Simples Nacional.*
>
> *Para entenderem o impacto financeiro do nosso sistema: empresas intelectuais e de tecnologia — como software houses, clínicas médicas, escritórios de engenharia e consultorias — começam sendo tributadas por padrão no temido **Anexo V**, cuja alíquota inicial é de **15,50%**.*
>
> *Entretanto, a Lei Complementar 123 estabelece que, se a razão entre a folha de pagamento dos últimos 12 meses e a receita bruta acumulada no mesmo período for **igual ou superior a 28%**, essa mesma empresa tem o direito legal de migrar para o **Anexo III**, onde a alíquota cai para a partir de **6,00%**.*
>
> *Uma empresa que fatura R\$ 50.000,00 por mês pode economizar mais de **R\$ 57.000,00 reais por ano em impostos** apenas mantendo o Fator R sob controle. Porém, acompanhar isso mês a mês em planilhas manuais gera erros de arredondamento, esquecimento de competências e surpresas com multas ou desenquadramentos.*
>
> *Para resolver isso com nível bancário de robustez, arquitetamos o sistema sobre:*
> - *No Backend: **Java 21 LTS com Spring Boot 4.0.6**, com Hibernate ORM e arquitetura em camadas limpas;*
> - *No Frontend: **Angular 21.2** com Standalone Components, tipagem estrita no TypeScript 5.9 e reatividade com RxJS;*
> - *No Banco de Dados: **PostgreSQL 16 Alpine**, conteinerizado via Docker Compose.*
>
> *Além disso, implementamos o **novo padrão oficial da Receita Federal do Brasil de CNPJ Alfanumérico**, com suporte pleno a 14 caracteres alfanuméricos em todo o ecossistema, além de higienização de CPF.*
>
> *Vamos agora direto para o protótipo funcional rodando em tempo real."*

---

### 🖥️ Ação ao Vivo no Sistema (Apresentador 1):
1. **Ir para a tela de Login (`http://localhost:4200/login`)**:
   - Mostrar os campos: Login com CPF ou E-mail e Senha.
   - Digitar o CPF do Contador: `11122233344` e Senha: `123456`.
   - Clicar em **Entrar**.
2. **Explicar o Token JWT e RBAC**:
   - Apontar que a API gerou um token JWT HMAC-SHA256 e o Frontend utiliza um `HttpInterceptor` funcional injetando o Bearer Token em todas as chamadas.
3. **No Header Superior (Troca de Empresa Ativa)**:
   - Mostrar o seletor de contexto de empresa (`EmpresaContextService`):
   - Alternar entre **Empresa Alpha Serviços** e **Empresa Beta Tecnologia**.
   - Mostrar como o dashboard reage instantaneamente sem recarregar a página (reatividade RxJS).
4. **Visão do Dashboard (`/home`)**:
   - Apontar o **Card do Fator R Atual** (ex: `28.45% - ANEXO III`).
   - Apontar o badge de status e os **Alertas Preventivos de Margem**:
     - Zona de Risco (28% a 31%): perigo de queda.
     - Zona de Oportunidade (25% a 27.99%): oportunidade de subir a folha para economizar.

---

### 🔄 Frase de Transição do Apresentador 1 para o Apresentador 2:
> *"Agora que vimos a base estratégica e como o sistema isola a governança de acesso e multi-empresas, convido o **[Nome do Apresentador 2]**, que vai demonstrar como alimentamos a inteligência tributária do sistema a partir do mundo real: a captura automática de Notas Fiscais Eletrônicas e a conciliação bancária inteligente."*

---

## 👤 PARTE 2: Operações Financeiras, Automação de Dados & Conciliação
**Responsável**: Apresentador 2  
**Tempo**: `05:00` a `10:30` (5.5 minutos)  
**Telas Utilizadas**: Contas a Receber (`/contas-a-receber`) / Banking e Conciliação (`/banking`)

### 🎯 Objetivos Desta Etapa:
1. Demonstrar a **Trilha 2**: Módulo de Contas a Receber e Parser XML de NF-e/NFS-e.
2. Ressaltar a segurança da engenharia: proteção contra ataques XXE (*XML External Entity*) e validação de titularidade da empresa.
3. Mostrar a pré-visualização de títulos antes de gravar no banco e o auto-cadastro de tomadores.
4. Demonstrar a **Trilha 3**: Módulo de Banking com importação de extratos OFX e CSV, deduplicação por FITID e a interface split-view com sugestões de match e desconciliação em 1 clique.

---

### 🎙️ Roteiro de Fala Sugerido (Apresentador 2):

> *"Obrigado, [Nome do Apresentador 1].  
> Para que o cálculo do Fator R seja matematicamente confiável, o sistema não pode depender de digitação manual de faturamento. Por isso, na **Trilha 2**, implementamos a automação direta de documentos fiscais eletrônicos.
>
> *Aqui na tela de **Contas a Receber**, temos a gestão analítica de títulos financeiros com controle rigoroso de status: títulos pendentes, pagos e cancelados.
>
> *O grande diferencial técnico está no botão **Importar XML de NF-e / NFS-e**. Desenvolvemos um parser seguro em Java no `LeitorXmlNfeService` que desabilita DTD externa para blindar a API contra injeções de segurança do tipo XXE (XML External Entity).
>
> *Além disso, o backend valida se o CNPJ emitente da nota confere com a empresa ativa no sistema, impedindo que um usuário importe notas de terceiros por engano. Ao ler a nota, o sistema auto-cadastra o cliente tomador na tabela `clientes_fornecedores` e abre uma tela de **Pré-Visualização** com o valor das duplicatas para o usuário conferir antes de persistir."*

---

### 🖥️ Ação ao Vivo no Sistema (Apresentador 2 - Parte A: Contas a Receber):
1. **Na tela `/contas-a-receber`**:
   - Mostrar a tabela com filtros de busca, ordenação e status de títulos.
   - Clicar em **Importar XML**.
   - Selecionar um arquivo XML da pasta:  
     `arquivos para testes/faturamento/01 - Inovar Softwares/2026/02/` (ou de qualquer mês).
   - **Destacar para a banca**:
     - O modal de **Pré-visualização** abre instantaneamente exibindo: Número da Nota, Tomador, Vencimento e Valor Líquido.
     - Clicar em **Confirmar / Salvar**: O título surge na listagem com badge azul `PENDENTE`.
   - Mostrar a **Baixa Manual**: clicar em "Baixar Título", preencher a data de pagamento e confirmar. O status vira `PAGO` instantaneamente.

---

### 🎙️ Continuação da Fala (Apresentador 2 - Parte B: Banking & Conciliação):

> *"Mas como garantir que essa receita realmente entrou no caixa da empresa e não é uma nota fiscal cancelada ou inadimplente? É aqui que entra a **Trilha 3: Módulo Banking & Conciliação Bancária**.
>
> *O sistema integra diretamente com o sistema financeiro bancário através do padrão de mercado **OFX (Open Financial Exchange)** e também arquivos **CSV**.
>
> *Para evitar que um mesmo extrato gere duplicidade de lançamentos, implementamos um motor de deduplicação por chave unívoca **FITID** (Financial Transaction ID). Se o banco emitir lançamentos repetidos, o nosso backend descarta a duplicação na hora.
>
> *Na interface, desenhamos uma visualização moderna em **Split-View**: do lado esquerdo, os lançamentos que vieram do banco; do lado direito, os títulos a receber do sistema.
>
> *Notem que o sistema faz um **algoritmo de correspondência inteligente**: ele identifica lançamentos bancários com o mesmo valor e data próxima aos títulos pendentes e sugere o pareamento automático para o operador contábil."*

---

### 🖥️ Ação ao Vivo no Sistema (Apresentador 2 - Parte B: Banking):
1. **Navegar para a tela `/banking`**:
   - Clicar em **Importar Extrato**.
   - Fazer upload de um arquivo OFX ou CSV da pasta:  
     `arquivos para testes/extrato bancario/01 - Inovar Softwares/2026/02/extrato_2026_02.ofx`
   - O extrato é importado e os lançamentos aparecem na coluna da esquerda.
2. **Realizar a Conciliação ao Vivo**:
   - Clicar em um lançamento do extrato (ex: Crédito de R\$ 15.000,00).
   - O sistema destaca os títulos correspondentes na coluna direita.
   - Clicar no botão **Conciliar**.
   - O lançamento recebe o badge verde `CONCILIADO` e o vínculo atômico transacional é registrado no banco.
3. **Demonstrar a Desconciliação**:
   - Mostrar a aba **Histórico de Conciliações**.
   - Clicar no botão **Desconciliar** com 1 clique: o sistema estorna a transação com atomicidade (`@Transactional`), desfaz o vínculo e devolve o título para o status original.

---

### 🔄 Frase de Transição do Apresentador 2 para o Apresentador 3:
> *"Com as receitas auditadas e conciliadas contra os extratos reais, chegamos ao coração da inteligência fiscal do projeto. Passo agora a palavra para o **[Nome do Apresentador 3]**, que apresentará a automação da folha de pagamento via eSocial, a nossa calculadora do Pró-Labore ideal e o dossiê final de auditoria com assinatura digital SHA-256."*

---

## 👤 PARTE 3: Motor Fiscal, Inteligência de Pró-Labore & Dossiê de Auditoria
**Responsável**: Apresentador 3  
**Tempo**: `10:30` a `16:00` (5.5 minutos)  
**Telas Utilizadas**: Folha de Pagamento (`/payroll`) / Simulador de Cenários (`/simulador`) / Central de Relatórios & Laudos (`/reports`)

### 🎯 Objetivos Desta Etapa:
1. Demonstrar a **Trilha 4**: Importação de eventos do eSocial (**S-1200 / S-1210**) e sincronização atômica com o Fator R.
2. Apresentar a **Calculadora de Pró-Labore Ideal**: demonstrar como o sistema calcula o valor cirúrgico de pró-labore para cravar os 28% no Anexo III, ponderando o custo de INSS e IRPF.
3. Mostrar a **Simulação de Cenários Futuros** ($M+1$ e $M+2$) nos cenários Otimista (+15%), Neutro e Pessimista (-15%) contra o Lucro Presumido.
4. Demonstrar a **Trilha 5**: Emissão do Laudo Tributário formal em PDF com CRC do contador via OpenPDF, exportação em Excel via Apache POI e verificação de autenticidade por **Hash SHA-256**.

---

### 🎙️ Roteiro de Fala Sugerido (Apresentador 3):

> *"Obrigado, [Nome do Apresentador 2].  
> Se por um lado a receita compõe o denominador da fração do Fator R, o numerador é a **Folha de Pagamento**.
>
> *Na **Trilha 4**, implementamos a integração oficial com o **eSocial do Governo Federal**. O nosso backend processa diretamente os arquivos XML dos eventos **S-1200** (Remuneração do Trabalhador) e **S-1210** (Pagamentos de Rendimentos), além de planilhas de sistemas contábeis como Domínio e Alterdata.
>
> *O sistema discrimina perfeitamente o que é Salário CLT de colaboradores e o que é Pró-Labore de sócios. Ao importar a folha, o sistema executa uma **sincronização atômica** no banco: a competência do Fator R é atualizada no mesmo milissegundo.
>
> *Mas a funcionalidade mais inovadora do sistema é o **Simulador de Pró-Labore Mínimo Ideal**.  
> Muitos empresários aumentam o pró-labore às cegas para tentar atingir os 28% do Anexo III, mas acabam perdendo todo o dinheiro pagando alíquotas de até 27,5% de Imposto de Renda Pessoa Física e 11% de INSS.*
>
> *O nosso algoritmo resolve essa equação por otimização: ele calcula exatamente quanto falta para atingir 28,00%, simula o custo previdenciário e fiscal do sócio (INSS + IRPF) e compara com a economia de Simples Nacional gerada. Se a economia fiscal for superior ao custo do pró-labore, o sistema emite a recomendação: **'Ajuste Viável - Economia Real'**."*

---

### 🖥️ Ação ao Vivo no Sistema (Apresentador 3 - Parte A: Payroll & Pró-Labore):
1. **Navegar para a tela `/payroll`**:
   - Mostrar a tabela de folhas analíticas cadastradas.
   - Clicar em **Importar Folha / eSocial**.
   - Selecionar um arquivo XML do eSocial da pasta:  
     `arquivos para testes/folha de pagamento/01 - Inovar Softwares/2026/02/esocial_s1200_2026_02.xml`
   - O arquivo é lido e integrado imediatamente aos totais da empresa.
2. **Demonstrar a Calculadora de Pró-Labore**:
   - Apontar o card **"Otimizador de Pró-Labore para o Anexo III"**:
   - Destacar os números em tempo real:
     - Fator R atual da empresa (ex: `24.50%` - Anexo V).
     - Pró-Labore adicional recomendado: (ex: `+ R$ 2.450,00`).
     - Novo Fator R projetado: `28.05%` (Migração para Anexo III).
     - Economia Líquida Estimada no Simples Nacional após descontar INSS/IRPF.
3. **Navegar para o `/simulador`**:
   - Mostrar as projeções futuras de **M+1 e M+2** nos três cenários: Otimista (+15% de receita), Neutro e Pessimista (-15%).
   - Mostrar a tabela comparativa contra o **Lucro Presumido** (PIS, COFINS, ISS, IRPJ, CSLL e adicional), provando onde a empresa paga menos imposto.

---

### 🎙️ Continuação da Fala (Apresentador 3 - Parte B: Dossiê e Laudo SHA-256):

> *"Por fim, na **Trilha 5**, transformamos toda essa inteligência analítica em um instrumento com validade jurídica e contábil: o **Dossiê Tributário com Emissão de Parecer Técnico**.
>
> *Utilizando a biblioteca **OpenPDF**, o backend gera em tempo de execução um laudo técnico completo e timbrado contendo os dados do escritório contábil, o número de registro no **CRC do contador responsável**, a memória de cálculo dos 12 meses, as notas fiscais e a fundamentação legal da LC 123.*
>
> *Para garantir a conformidade e a integridade da auditoria, todo laudo gerado recebe um **Certificado de Autenticidade com Hash SHA-256 criptográfico**.
>
> *Esse hash é registrado no banco de dados. Qualquer fiscal da Receita Federal, banco ou sócio pode pegar o código do documento ou o arquivo e validá-lo no validador público do sistema para comprovar que o parecer não sofreu nenhuma adulteração.*
>
> *Além do PDF, o sistema utiliza o **Apache POI** para gerar a planilha completa em formato `.xlsx` com formatação corporativa e fórmulas dinâmicas para o time financeiro trabalhar."*

---

### 🖥️ Ação ao Vivo no Sistema (Apresentador 3 - Parte B: Reports):
1. **Navegar para a tela `/reports`**:
   - Selecionar a empresa ativa e a competência de referência (ex: `2026-02`).
   - Clicar em **Emitir Laudo em PDF**:
     - O PDF abre no pré-visualizador incorporado no navegador.
     - **Mostrar na tela**: cabeçalho timbrado, CRC do contador, memória analítica dos 12 meses, demonstrativo de economia anual e o **código Hash SHA-256** no rodapé.
   - Clicar em **Exportar Planilha Excel (.xlsx)**:
     - Abrir ou mostrar o download do arquivo com abas detalhadas.
   - Mostrar a funcionalidade de **Validação de Laudo por Hash**:
     - Copiar o Hash SHA-256 gerado e colar no campo de verificação de autenticidade: o sistema retorna o selo verde **"DOCUMENTO AUTÊNTICO E AUDITADO"**.

---

### 🔄 Conclusão Conjunta (Todos os Apresentadores):
**Apresentador 3 passa para os 3 se posicionarem juntos**:

> *"Para concluir: o Sistema de Controle Fator R não é apenas uma calculadora de alíquotas. Ele é uma plataforma corporativa completa que conecta o mundo fiscal real das notas e do eSocial à inteligência estratégica do empresário e do contador, blindando a empresa contra erros e gerando economia tributária comprovada.
>
> *Agradecemos a atenção de todos e abrimos agora para perguntas e considerações da banca avaliadora."*

---

# 🎓 FAQ Técnico & Fiscal para a Banca (Perguntas e Respostas Prontas)

Caso a banca ou os avaliadores façam perguntas aprofundadas, utilizem as respostas abaixo:

---

### ❓ 1. "Como o sistema trata empresas recém-abertas com menos de 12 meses de vida?"
> **Quem responde**: Apresentador 1 ou 3  
> **Resposta Técnica**:  
> *"Conforme a Lei Complementar 123/2006, empresas com 1 a 11 meses de atividade utilizam a regra da **proporcionalização de RBT12 por média móvel**. O nosso `CalculoFatorRService` detecta a data de abertura da empresa. Se ela tem 3 meses, por exemplo, o sistema soma o faturamento dos 3 meses, divide por 3 para achar a média mensal e multiplica por 12 para encontrar a base anualizada proporcional de RBT12. O mesmo cálculo é aplicado à folha de pagamento para apurar o Fator R de forma justa."*

---

### ❓ 2. "Por que escolheram Java 21 com Spring Boot 4 no Backend e Angular 21 no Frontend?"
> **Quem responde**: Apresentador 1  
> **Resposta Técnica**:  
> *"Por se tratar de um sistema financeiro e fiscal com precisão monetária (`BigDecimal`) e operações atômicas, o **Java 21 LTS** nos oferece tipagem estrita, performance de ponta e suporte aos novos Virtual Threads. O **Spring Boot 4** provê um ecossistema maduro para transações bancárias `@Transactional` e segurança integrada. No frontend, o **Angular 21** com Standalone Components elimina a sobrecarga dos módulos legados, garante contratos rígidos via TypeScript 5.9 e reatividade fluida com RxJS, essencial para manter o estado da empresa ativa sincronizado em toda a aplicação."*

---

### ❓ 3. "Como é feita a segurança contra arquivos maliciosos na importação de XML (XXE)?"
> **Quem responde**: Apresentador 2  
> **Resposta Técnica**:  
> *"No nosso `LeitorXmlNfeService`, configuramos o `DocumentBuilderFactory` explicitamente desabilitando DTDs externas (`disallow-doctype-decl = true`, `external-general-entities = false` e `external-parameter-entities = false`). Isso neutraliza por completo vetores de ataque XXE (XML External Entity), impedindo que um XML forjado acerte arquivos locais do servidor ou faça SSRF."*

---

### ❓ 4. "Como funciona a garantia de integridade do Laudo Tributário via SHA-256?"
> **Quem responde**: Apresentador 3  
> **Resposta Técnica**:  
> *"Quando o relatório é consolidado no `ReportsService`, montamos a representação canônica dos dados daquele laudo (empresa, competência, faturamento consolidado, folha consolidada, alíquota apurada e carimbo de tempo). Aplicamos a função hash criptográfica `SHA-256` sobre essa assinatura e gravamos o resultado na tabela `laudos_tributarios`. Caso uma vírgula seja alterada na base de dados ou no PDF, o hash resultante muda drasticamente e a validação do documento aponta inconsistência."*

---

### ❓ 5. "Por que o CNPJ é armazenado como VARCHAR(20) e não numérico ou BigInteger?"
> **Quem responde**: Apresentador 1 ou 2  
> **Resposta Técnica**:  
> *"Por determinação oficial da **Receita Federal do Brasil**, o padrão de CNPJ brasileiro passou a ser **alfanumérico** (com letras e números, como `12ABC345000101`). Se usássemos tipos numéricos, o sistema se tornaria obsoleto perante a nova legislação. Por isso, padronizamos o CNPJ em `VARCHAR(20)` e criamos utilitários bidirecionais de máscara e validação tanto em Java (`CnpjUtils`) quanto em TypeScript (`cnpj.ts`)."*

---

## 📋 Resumo Visual das Credenciais de Demonstração

| Perfil | Usuário / Nome | Identificador (CPF / Login) | Senha | O que demonstrar |
| :--- | :--- | :--- | :--- | :--- |
| **Contabilista** | Contador Chefe | `11122233344` | `123456` | Acesso total, troca de empresa, aprovação de vínculos, emissão de laudo com CRC. |
| **Empresário** | Empresário Silva | `22233344455` | `123456` | Visão das suas empresas (Alpha e Beta), dashboards e simulações. |
| **Colaborador** | Colaborador Convidado | `99988877766` | *(Aprovação)* | Fluxo de aprovação de vínculo pelo contador. |

---

*Boa apresentação a todos! O roteiro foi construído para transmitir segurança técnica, domínio contábil e impacto real de negócio.*
