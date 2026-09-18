# 📊 Sistema de Controle Fator R

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-21.2-dd0031?logo=angular)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue?logo=typescript)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Vitest](https://img.shields.io/badge/Vitest-4.1-yellow?logo=vitest)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ed?logo=docker)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📌 Visão Geral & Problema de Negócio

O **Sistema de Controle Fator R** é uma plataforma corporativa *full-stack* desenvolvida para automatizar o planejamento, enquadramento e auditoria tributária de empresas prestadoras de serviços enquadradas no **Simples Nacional** (conforme Lei Complementar nº 123/2006 e resoluções do CGSN).

### 💡 Por que o sistema foi criado?
Atividades intelectuais e de serviços (como desenvolvimento de software, consultoria em TI, clínicas médicas, engenharia e arquitetura) podem ser tributadas pelo **Anexo III** (alíquotas a partir de 6%) ou pelo **Anexo V** (alíquotas a partir de 15,50%).

O enquadramento é determinado pela razão matemática do **Fator R**:
$$\text{Fator R} = \frac{\sum \text{Folha de Pagamento (Últimos 12 Meses)}}{\sum \text{Receita Bruta Total (Últimos 12 Meses)}}$$

- **Fator R $\ge$ 28%:** Tributação pelo **Anexo III** (Grande economia tributária).
- **Fator R < 28%:** Tributação pelo **Anexo V** (Carga fiscal mais onerosa).

### 🚀 O que o sistema resolve:
1. **Apuração Contínua do Fator R:** Calcula a proporção exata considerando a janela deslizante de 12 meses anteriores ($M-12$ a $M-1$).
2. **Proporcionalização de RBT12:** Aplica a regra da média para empresas com menos de 12 meses de abertura.
3. **Alertas Preventivos de Margem:** Emite alertas na zona de oportunidade (25% a 27,99%) e na zona de risco de rebaixamento (28% a 31%).
4. **Comparativo com Lucro Presumido:** Apura PIS, COFINS, ISS, IRPJ, CSLL e adicional, apontando a economia anual estimada.
5. **Projeções de Cenários Futuros:** Projeta o impacto nos meses $M+1$ e $M+2$ para cenários Otimista (+15%), Neutro (0%) e Pessimista (-15%).
6. **Automação de Receitas & Despesas:**
   - **Receitas:** Importação de XML de NF-e/NFS-e com auto-cadastro de tomadores e geração de contas a receber.
   - **Bancos:** Importação de extratos OFX/CSV e conciliação bancária manual em split-view.
   - **Folha:** Importação de eventos do eSocial (S-1200 / S-1210) e relatórios de ERPs contábeis, com recomendador de pró-labore ideal.
7. **Dossiê & Parecer Técnico:** Emissão de laudos formais em PDF timbrado com CRC do contador e planilhas analíticas em Excel (`.xlsx`).

---

## 🏛️ Padrões Regulatórios e Arquiteturais Obrigatórios

### 1. CNPJ Alfanumérico (Padrão Oficial RFB)
Conforme a nova regulamentação da Receita Federal do Brasil, todos os campos de CNPJ utilizam o tipo `VARCHAR(20)` para acomodar caracteres alfanuméricos (letras e números):
- **Formato Válido:** 14 caracteres alfanuméricos `^[A-Z0-9]{14}$` (ex: `12ABC345000101`, `98XYZ765000102`).
- **Máscara Padrão:** `XX.XXX.XXX/XXXX-XX`.
- **Utilitários Globais:** Disponíveis em `src/shared/utils/cnpj.ts` (TypeScript) e `com.fatorr.backend.util.CnpjUtils` (Java):
  - `maskCnpj(valor)`: Aplica a máscara alfanumérica.
  - `unmaskCnpj(valor)`: Limpa caracteres especiais e retorna string em caixa alta.
  - `isValidCnpj(valor)`: Valida tamanho (14) e formato alfanumérico.

### 2. CPF Padronizado Estritamente Numérico
Para evitar discrepâncias em consultas e autenticação:
- CPFs são armazenados e trafegados no banco **estritamente com 11 dígitos numéricos** (`VARCHAR(14)`), sem pontos ou traços.
- Máscaras (`000.000.000-00`) são aplicadas apenas na camada de exibição visual.

---

## 👥 Organização da Equipe (5 Trilhas Paralelas)

O desenvolvimento das novas funcionalidades está distribuído em **5 trilhas isoladas** para garantir que desenvolvedores atuem em paralelo sem conflitos de merge:

| Trilha | Branch de Trabalho | Domínio / Módulo | Escopo Principal |
| :---: | :--- | :--- | :--- |
| **1** | `feature/auth-rbac-empresas` | `src/modules/auth/` | Login JWT, Refresh Token, RBAC (Contador, Empresário, Usuário), pré-cadastro por CPF e aprovação de acessos. |
| **2** | `feature/contas-a-receber-xml` | `src/modules/receivables/` | CRUD de títulos, baixa manual, parser XML de NF-e/NFS-e e auto-cadastro de tomadores. |
| **3** | `feature/extrato-conciliacao-manual` | `src/modules/banking/` | Parser OFX/CSV, deduplicação por FITID, tela split-view e conciliação transacional. |
| **4** | `feature/folha-pagamento-esocial` | `src/modules/payroll/` | Parser eSocial (S-1200/S-1210), importação de folha CSV/Excel e simulador de Pró-Labore ideal. |
| **5** | `feature/dossie-relatorios-tributarios` | `src/modules/reports/` | Motor de geração de Laudos em PDF com CRC, exportação Excel `.xlsx` e autenticidade por hash SHA-256. |

> 📖 Para instruções completas de desenvolvimento, convenções de commit e regras de arquitetura, consulte o [**Guia da Equipe (`TASKS_GUIDE.md`)**](./TASKS_GUIDE.md).

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Linguagem & Framework:** Java 21 LTS / Spring Boot 4.0.6
- **Módulos Spring:** Spring WebMVC, Spring Data JPA, Jakarta Bean Validation
- **Persistência:** Hibernate ORM 7.2 & PostgreSQL JDBC Driver
- **Migrações de Banco:** Scripts SQL versionados (`V1`, `V2`, `V3`)
- **Utilitários:** Project Lombok, Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Testes:** JUnit 5, Mockito

### Frontend
- **Framework:** Angular 21.2 (Standalone Components)
- **Gerenciamento de Estado:** RxJS 7.8 (BehaviorSubject / Observables)
- **Linguagem:** TypeScript 5.9
- **Estilização:** CSS3 puro e responsivo com design tokens modernos
- **Testes:** Vitest 4.1 & JSDOM

### Shared & Contratos Universais
- **Contratos de Tipos:** TypeScript Interfaces compartilhadas (`auth`, `receivables`, `banking`, `payroll`, `reports`, `common`)
- **Utilitários Globais:** Sanitização de CNPJ alfanumérico, CPF, formatação monetária (BRL) e manipuladores de datas

### Infraestrutura & Banco de Dados
- **Banco de Dados:** PostgreSQL 16 Alpine
- **Containerização:** Docker & Docker Compose

---

## 📂 Estrutura de Pastas do Projeto

```plaintext
SISTEMA DE CONTROLE FATOR R/
├── .env.example                     # Template de variáveis de ambiente
├── .gitignore                       # Ignora builds, dependências, segredos e temporários
├── docker-compose.yml               # Orquestração do PostgreSQL 16
├── TASKS_GUIDE.md                   # Guia oficial de desenvolvimento e divisão das 5 trilhas
├── DOCUMENTATION.md                 # Documentação técnica de engenharia
├── documentacao.html                # Documentação técnica interativa autocontida
├── README.md                        # Guia principal do repositório
│
├── src/                             # Fundações e Módulos Globais
│   ├── shared/                      # Contratos e utilitários universais
│   │   ├── types/                   # Contratos TS (auth, receivables, banking, payroll, reports)
│   │   ├── utils/                   # Utilitários (cnpj alfanumérico, date, formatters)
│   │   ├── database/                # Schemas SQL e Seeds de teste
│   │   └── middlewares/             # AuthGuard, TenantMiddleware, ErrorHandler
│   │
│   └── modules/                     # Scaffolding das 5 Trilhas de Desenvolvimento
│       ├── auth/                    # [Trilha 1] Login, RBAC e Multi-Empresas
│       ├── receivables/             # [Trilha 2] Contas a Receber e Parser XML
│       ├── banking/                 # [Trilha 3] Extratos OFX/CSV e Conciliação
│       ├── payroll/                 # [Trilha 4] Folha Detalhada, eSocial e Pró-Labore
│       └── reports/                 # [Trilha 5] Laudos PDF, Excel e Dossiê Fiscal
│
├── backend/                         # Módulo API REST (Spring Boot 4.0.6)
│   ├── mvnw / mvnw.cmd              # Wrapper do Maven
│   ├── pom.xml                      # Dependências Maven
│   └── src/
│       ├── main/java/com/fatorr/backend/
│       │   ├── BackendApplication.java    # Bootstrap da aplicação Spring Boot
│       │   ├── config/DataSeeder.java     # Carga de parâmetros e seeds iniciais
│       │   ├── controller/                # Endpoints REST (Auth, Empresa, Faturamento, etc.)
│       │   ├── domain/                    # Entidades JPA (13 tabelas relacionais)
│       │   ├── dto/                       # Data Transfer Objects
│       │   ├── repository/                # Interfaces Spring Data JPA
│       │   ├── service/                   # Cálculos fiscais e regras de negócio
│       │   └── util/                      # CnpjUtils e validadores Java
│       └── resources/
│           ├── application.properties     # Configurações de banco e Hibernate
│           └── db/migration/              # Migrações SQL versionadas (V1, V2, V3)
│
└── frontend/                        # Módulo SPA (Angular 21.2)
    ├── package.json                 # Dependências Node.js / Angular
    ├── tsconfig.json                # Configurações TypeScript
    └── src/
        ├── environments/            # Configurações de endpoints (dev/prod)
        └── app/
            ├── app.routes.ts        # Rotas da aplicação e Guards de segurança
            ├── layout/              # Layout principal (Header, menu e contexto de empresa)
            ├── models/              # Interfaces TypeScript do frontend
            ├── pages/               # Telas (Home, Login, Simulador, Empresas, Parâmetros)
            ├── services/            # Serviços HTTP e RxJS State Context
            └── shared/              # Utilitários e types sincronizados
```

---

## 🗄️ Esquema do Banco de Dados (13 Tabelas)

O banco de dados relacional (PostgreSQL) é composto pelas seguintes tabelas estruturadas:

1. `organizacoes_contabeis`: Escritórios de contabilidade (CNPJ alfanumérico, CRC).
2. `usuarios`: Usuários com perfis RBAC (`CONTABILISTA`, `EMPRESARIO`, `USUARIO`), status e credenciais.
3. `empresas`: Empresas clientes vinculadas a contabilidades (CNPJ alfanumérico).
4. `empresa_usuarios`: Vínculo NxN com controle de função e aprovação de acesso.
5. `clientes_fornecedores`: Tomadores e prestadores de serviços de cada empresa.
6. `contas_a_receber`: Títulos financeiros gerados manualmente ou via XML de notas.
7. `extrato_lancamentos`: Lançamentos bancários importados via OFX/CSV com controle de FITID.
8. `conciliacoes_bancarias`: Registro da conciliação transacional entre extrato e título.
9. `folhas_detalhadas`: Folha de pagamento analítica (salários, pró-labore, 13º, encargos e eSocial).
10. `laudos_tributarios`: Registro de pareceres técnicos com hash SHA-256 para auditoria.
11. `faturamentos_mensais`: Histórico mensal de receitas brutas da empresa.
12. `folhas_pagamento`: Histórico mensal consolidado de folhas para o Fator R.
13. `parametros_tributarios` & `faixas_tributarias`: Tabelas de alíquotas e faixas progressivas dos Anexos III e V.

---

## ⚙️ Guia de Instalação e Execução

### Pré-requisitos
- [Git](https://git-scm.com/) (v2.x+)
- [Docker & Docker Compose](https://www.docker.com/)
- [Java Development Kit (JDK 21)](https://adoptium.net/)
- [Node.js (v20.x LTS)](https://nodejs.org/) & `npm` (v10.x+)

---

### Passo 1: Clonar o Repositório e Configurar Variáveis
```bash
git clone https://github.com/WerneyPark/SISTEMA-DE-CONTROLE-FATOR-R.git
cd SISTEMA-DE-CONTROLE-FATOR-R

# Copiar arquivo de variáveis de ambiente
cp .env.example .env
```

---

### Passo 2: Subir o Banco de Dados (PostgreSQL)
```bash
docker-compose up -d
```
> O container `fatorr_db` inicializará na porta `5432` com banco `fatorr`, usuário `postgres` e senha `admin`.

---

### Passo 3: Executar o Backend (Spring Boot)
```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
> A API estará disponível em: **`http://localhost:8080`**.  
> Na primeira inicialização, o `DataSeeder` aplicará automaticamente os parâmetros tributários e a carga de dados de teste (Seeds).

---

### Passo 4: Executar o Frontend (Angular)
Abra outro terminal:
```bash
cd frontend
npm install
npm start
```
> Acesse a aplicação web no navegador em: **`http://localhost:4200`**.

---

## 🔑 Credenciais e Dados de Teste Locais (Seeds)

O banco é inicializado automaticamente com os seguintes usuários e empresas para testes locais:

| Perfil | Nome | Identificador (CPF / Login) | Senha | Vínculos / Organização |
| :--- | :--- | :--- | :--- | :--- |
| **Contabilista (Admin)** | Contador Chefe | `11122233344` | `123456` | Organização Contábil Modelo Ltda (`12ABC345000199` / `CRC-SP 123456/O`) |
| **Empresário** | Empresário Silva | `22233344455` | `123456` | Vinculado à Empresa Alpha (`12ABC345000101`) e Empresa Beta (`98XYZ765000102`) |
| **Colaborador** | Colaborador Convidado | `99988877766` | *(Pendente)* | Vínculo pendente na Empresa Alpha |
| **Admin Master Legado** | Administrador | `00000000000` | `admin123` | Acesso global aos Parâmetros Padrão |

---

## 🧪 Execução de Testes Automatizados

### Testes do Backend (JUnit 5 / Mockito)
Executa os testes unitários de cálculo do Fator R, enquadramento de faixas, proporcionalidade de RBT12 e utilitários de CNPJ/CPF:
```bash
cd backend

# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Testes do Frontend e Contratos Universais (Vitest)
Executa a suíte de testes de validação do CNPJ alfanumérico, formatação monetária, manipulação de datas e contratos de folha e relatórios:
```bash
cd frontend

# Executar testes uma única vez
npx vitest run

# Executar em modo watch durante desenvolvimento
npm test
```

---

## 📄 Licença

Este projeto está sob a licença **MIT** - consulte o arquivo de licença para mais detalhes.

---

## 👨‍💻 Autoria & Mantenedores

Desenvolvido para gestão tributária, inteligência contábil e controle analítico de empresas prestadoras de serviços do Simples Nacional.
