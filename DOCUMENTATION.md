# Documentação Técnica: Sistema de Controle Fator R

## 1. Visão Geral

O **Sistema de Controle Fator R** é uma aplicação web full-stack corporativa desenvolvida para calcular e gerenciar o **Fator R** de empresas prestadoras de serviço (frequentemente na área de tecnologia, clínicas, engenharias) enquadradas no regime tributário do Simples Nacional no Brasil. 

### Problema que Resolve
No Simples Nacional, certas atividades de serviço podem ser tributadas pelo **Anexo III** (alíquotas mais baixas) ou pelo **Anexo V** (alíquotas mais altas). A regra que define qual anexo usar é o **Fator R**. Se a razão entre a folha de pagamento (salários, pró-labore, FGTS, INSS) e a receita bruta dos últimos 12 meses for **igual ou superior a 28%**, a empresa é tributada pelo Anexo III, gerando grande economia fiscal. Se for menor, cai no Anexo V.
Este sistema automatiza o acompanhamento desse percentual, alertando sobre a proximidade da margem e projetando cenários comparativos contra outros regimes tributários, como o Lucro Presumido.

### Arquitetura de Alto Nível
- **Backend (API Rest):** Construído em **Java 21 com Spring Boot**. Contém toda a lógica e as regras fiscais complexas (proporcionalização de RBT12, enquadramento de faixas e cálculo de alíquotas efetivas).
- **Frontend (SPA):** Construído em **Angular 21**. Fornece a interface ao usuário com serviços modulares baseados no RxJS.
- **Banco de Dados:** Utiliza **PostgreSQL 16** via Docker Compose para a persistência relacional.

---

## 2. Dependências e Pré-requisitos

### Bibliotecas e Frameworks
- **Backend:**
  - `Java 21`
  - `Spring Boot 4.0.6` (Starter Web, Data JPA, Validation)
  - `PostgreSQL Driver`
  - `Lombok` (Geração de getters, setters, constructors)
  - Gerenciador de Pacotes: `Maven`
- **Frontend:**
  - `Node.js` (Recomendado 18.x ou 20.x)
  - `Angular CLI` (`^21.2.10`)
  - `Angular Core/Common/Forms/Router` (`^21.2.0`)
  - `RxJS` (`~7.8.0`)
  - `TypeScript` (`~5.9.2`)
  - Gerenciador de Pacotes: `npm 11.11.0`

### Requisitos de Ambiente Externo
- **Docker & Docker Compose:** Necessário para orquestração da imagem do PostgreSQL.
- **Portas de Rede em uso:**
  - Backend API: `8080` (padrão Spring Boot)
  - Frontend UI: `4200`
  - Banco de Dados PostgreSQL: `5432`

---

## 3. Instalação e Execução

Instruções exatas para inicializar o ambiente de desenvolvimento "do zero".

**Passo 1: Subir o Banco de Dados (PostgreSQL)**
Acesse a raiz do projeto e orquestre o container Docker:
```bash
cd "c:\PROGRAMAS\SISTEMA DE CONTROLE FATOR R"
docker-compose up -d
```
O banco será criado com o nome `fatorr`, usuário `postgres` e senha `admin`.

**Passo 2: Iniciar a API Backend (Spring Boot)**
```bash
cd backend
./mvnw spring-boot:run
```
*No Windows, você pode usar `mvnw.cmd spring-boot:run`. O Hibernate cuidará da criação de todas as tabelas (ddl-auto=update).*

**Passo 3: Iniciar o Frontend (Angular)**
Abra uma nova aba no terminal:
```bash
cd frontend
npm install
npm start
```
*Acesse a interface no seu navegador por meio de `http://localhost:4200`.*

---

## 4. Estrutura do Código

A base de código está logicamente separada em cliente e servidor.

### Backend (`/backend/src/main/java/com/fatorr/backend/`)
- `controller/`: Camada de Endpoints da API REST (Ex: `EmpresaController.java`, `FaturamentoMensalController.java`). Mapeia as rotas HTTP e converte respostas.
- `service/`: O "coração" da aplicação. Camada que isola toda a regra de negócio e lógica de cálculo do modelo tributário.
- `repository/`: Interfaces do Spring Data JPA (`JpaRepository`) responsáveis pelas consultas e persistência no PostgreSQL.
- `domain/`: Onde residem as entidades mapeadas via `@Entity` para o banco de dados (Ex: `Empresa`, `FaixaTributaria`).
- `dto/`: Objetos de Transferência de Dados que encapsulam os dados retornados para o Front-end, desvinculando-os diretamente do esquema de banco.

### Frontend (`/frontend/src/app/`)
- `services/`: Classes injetáveis contendo a comunicação HTTP com a API (`auth.service.ts`, `fator-r.service.ts`).
- `models/`: Interfaces TypeScript estritas usadas para tipar as repostas dos DTOs.
- `pages/` e `layout/`: Componentes visuais.
- `app.routes.ts`: Configuração do Angular Router.

### Padronização de Tipos de Dados (CPF e CNPJ como String)
- **CPF**:
  - Representado estritamente como `String` (backend `java.lang.String`, frontend TypeScript `string`, banco `VARCHAR(14)`).
  - Tráfego na API e armazenamento no banco de dados são feitos **sempre sem pontuação** (11 dígitos numéricos limpos).
  - Higienização e validação centralizadas via `CpfUtils` no backend (`unmask`, `mask`, `isValid`) e `formatters.ts` (`unmaskCpf`, `maskCpf`, `isValidCpf`) no frontend.
- **CNPJ**:
  - Representado estritamente como `String` (backend `java.lang.String`, frontend TypeScript `string`, banco `VARCHAR(20)`).
  - Suporta o novo padrão alfanumérico da Receita Federal do Brasil (14 caracteres alfanuméricos em caixa alta).
  - Higienização e validação centralizadas via `CnpjUtils` no backend e `cnpj.ts` no frontend.

---

## 5. Especificação Técnica (Classes, Módulos e Funções)

Abaixo encontra-se a análise linha por linha e especificação das classes centrais de negócio do sistema.

### `CalculoFatorRService.java`
Serviço principal da aplicação. Responsável pelo processamento pesado e simulações.

#### Função: `getParametros`
- **Propósito:** Recupera a tabela de alíquotas (parâmetros tributários) associada a uma empresa ou a tabela padrão do sistema.
- **Parâmetros de Entrada:** `Long empresaId` (Tipo `Long`, descreve o ID unívoco da empresa).
- **Retorno:** `ParametroTributario` - Objeto contendo os arrays de faixas do Simples Nacional e percentuais fixos.
- **Lógica Oculta:** Caso a empresa não possua parâmetros personalizados, o sistema faz um "fallback" para os parâmetros globais (`empresaIsNull()`). Caso não haja nenhum cadastrado, instancializa dinamicamente parâmetros na memória com alíquota de 16.33% (para não quebrar as projeções).

#### Função: `calcularFatorR`
- **Propósito:** Calcular a razão entre o somatório das folhas de pagamentos (pró-labores, salários) pelo somatório dos faturamentos auferidos nos **12 meses exatos que antecedem** a competência informada.
- **Parâmetros de Entrada:** 
  - `Long empresaId`: ID do cliente alvo.
  - `YearMonth competenciaReferencia`: Competência (mês e ano) que servirá de referencial de tempo da análise.
- **Retorno:** `FatorRResultadoDTO` - Representação da proporção encontrada em percentual (`BigDecimal`) e enquadramento de anexo (`String`).
- **Tratamento de Erros:** Dispara `IllegalArgumentException` se a empresa alvo não existir no banco. Evita `ArithmeticException` (Divisão por Zero) validando se o somatório do faturamento foi zero — e, neste cenário, por padrão, força o enquadramento no Anexo V sem cálculo de proporção.

#### Função: `simularTributacao`
- **Propósito:** Uma função complexa que não apenas calcula o Fator R, mas prevê as cobranças de DAS (Documento de Arrecadação do Simples Nacional) baseando-se no RBT12 vs Cobrança no Lucro Presumido.
- **Parâmetros de Entrada:** `Long empresaId` e `YearMonth competenciaReferencia`.
- **Lógica e Regras de Negócio:**
  1. **Análise de Alerta:** Checa se o Fator R está muito perto da margem de risco. Ativa `alertaProximidade` se estiver na banda de **25% até 27.99%** (podendo subir de anexo se subir um pouco a folha) ou **28% até 31%** (risco de cair de anexo caso a folha diminua).
  2. **Proporcionalidade de RBT12:** Se a empresa tem de 1 a 11 meses de abertura perante a Receita Federal, o sistema não considera apenas a soma bruta. O cálculo efetua média: `(Faturamento Acumulado / Meses de Atividade) * 12`.
  3. **Identificação da Faixa:** Itera sobre a lista de `FaixaTributaria` retornando a menor faixa cujo teto (`limiteSuperior`) consiga englobar o RBT12.
  4. **Cálculo da Alíquota Efetiva SN:** Diferente da Nominal, aplica-se `(RBT12 * Aliquota Nominal - Parcela a Deduzir) / RBT12`.
  5. **Verificação da 6ª Faixa:** Gera aviso para a interface informando que os impostos municipais e estaduais (ICMS/ISS) devem ser extraídos e cobrados fora do DAS caso a empresa seja identificada nesta faixa.
  6. **Cálculo da Economia:** Subtrai o valor a ser pago de `Lucro Presumido` do valor de `Simples Nacional`.
- **Retorno:** `SimulacaoTributariaDTO` - Um payload rico usado por contadores para tomada de decisão.

### `LeitorXmlNfeService.java`
Classe em desenvolvimento desenhada para interceptar faturamentos automatizados em formato de Documento Eletrônico.
- **Funções `processarXmlNfe` e `processarLote`:**
- Atualmente em fase de *Mock*, arremessando a exceção `UnsupportedOperationException`. Quando finalizada, exigirá parser usando Jackson XML para retirar atributos de `<vNF>` (Valor da Nota) e `<CNPJ>` de forma resiliente.

---

## 6. Exemplos de Uso

Abaixo constam payloads práticos trocados entre Frontend e Backend quando a simulação tributária é acionada.

### Simulando Cenário de Vantagem no Anexo III
Comando via cURL (Avaliando Competência de Maio de 2024):
```bash
curl -X GET "http://localhost:8080/api/fator-r/simulacao/1?competencia=2024-05"
```

**Payload de Resposta (Status 200 OK):**
```json
{
  "empresaId": 1,
  "razaoSocial": "Tech Consultoria Ltda",
  "cnpj": "12.345.678/0001-99",
  "competenciaReferencia": "2024-05",
  "mesesComDados": 12,
  "totalFaturamento12Meses": 100000.00,
  "totalFolha12Meses": 30000.00,
  "rbt12": 100000.00,
  "faixaEncontrada": 1,
  "mensagemFaixa6": null,
  "percentualFatorR": 30.00,
  "anexoAplicavel": "Anexo III",
  "alertaProximidadeLimite": true,
  "mensagemAlerta": "ATENÇÃO: O Fator R está em 30.00%, pouco acima do limite de 28%. Se a folha de pagamento diminuir, a empresa pode voltar ao Anexo V.",
  "aliquotaEfetivaSimplesNacional": 6.00,
  "valorEstimadoSimplesNacional": 600.00,
  "aliquotaEfetivaLucroPresumido": 16.33,
  "valorEstimadoLucroPresumido": 1633.00,
  "economiaEstimada": 1033.00,
  "regimeMaisVantajoso": "Simples Nacional",
  "mensagem": "Fator R igual ou superior a 28%. Tributação pelo Anexo III (mais vantajoso)."
}
```
**Análise do Resultado:** A empresa possui R$ 30.000 em folhas e R$ 100.000 em notas fiscais, rendendo exatos 30% de Fator R. Por este motivo, o backend enquadrou no "Anexo III" com uma alíquota efetiva muito inferior (6.00%) em relação ao Lucro Presumido (16.33%), além de retornar um aviso via `mensagemAlerta` de que ela está perto da zona de rebaixamento para o Anexo V.
