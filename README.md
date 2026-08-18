# 📊 Sistema de Controle Fator R

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-21.2-dd0031?logo=angular)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ed?logo=docker)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📌 Visão Geral & Problema de Negócio

O **Sistema de Controle Fator R** é uma plataforma corporativa *full-stack* desenvolvida para automatizar o planejamento e enquadramento tributário de empresas prestadoras de serviços enquadradas no **Simples Nacional** (conforme Lei Complementar nº 123/2006 e resoluções do CGSN).

### 💡 Por que o sistema foi criado?
Atividades intelectuais e de serviços (como desenvolvimento de software, consultoria em TI, clínicas médicas, engenharia e arquitetura) podem ser tributadas pelo **Anexo III** (alíquotas a partir de 6%) ou pelo **Anexo V** (alíquotas a partir de 15,50%).

O enquadramento é determinado pela razão matemática do **Fator R**:
$$\text{Fator R} = \frac{\sum \text{Folha de Pagamento (Últimos 12 Meses)}}{\sum \text{Receita Bruta Total (Últimos 12 Meses)}}$$

- **Fator R $\ge$ 28%:** Tributação pelo **Anexo III** (Grande economia tributária).
- **Fator R < 28%:** Tributação pelo **Anexo V** (Carga fiscal mais onerosa).

### 🚀 O que o sistema resolve:
1. **Apuração Contínua:** Calcula o percentual exato do Fator R com base nos 12 meses anteriores à competência informada.
2. **Proporcionalização de RBT12:** Aplica proporcionalidade pela média para empresas com menos de 12 meses de abertura.
3. **Alertas Preventivos de Margem:** Avisa proativamente quando a empresa está na faixa de oportunidade (25% a 27,99%) ou na faixa de risco de rebaixamento (28% a 31%).
4. **Comparativo com Lucro Presumido:** Apura todos os tributos federais e municipais do Lucro Presumido (PIS, COFINS, ISS, IRPJ presumido, CSLL presumida e adicional) e indica o regime mais vantajoso.
5. **Projeções de Cenários Futuros:** Projeta o impacto nos meses $M+1$ e $M+2$ para cenários Otimista (+15%), Neutro (0%) e Pessimista (-15%) com janela deslizante.

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Linguagem & Framework:** Java 21 LTS / Spring Boot 4.0.6
- **Módulos Spring:** Spring WebMVC, Spring Data JPA, Bean Validation
- **Persistência:** Hibernate ORM & PostgreSQL Driver
- **Utilitários:** Project Lombok, Maven Wrapper
- **Testes:** JUnit 5, Mockito

### Frontend
- **Framework:** Angular 21 (Standalone Components)
- **Gerenciamento de Estado:** RxJS 7.8 (BehaviorSubject / Observables)
- **Linguagem:** TypeScript 5.9
- **Estilização:** CSS3 puro e responsivo com design tokens modernos

### Infraestrutura & Banco de Dados
- **Banco de Dados:** PostgreSQL 16 Alpine
- **Containerização:** Docker & Docker Compose

---

## 📋 Pré-requisitos

Antes de iniciar, certifique-se de possuir instalado em sua máquina:
- [Git](https://git-scm.com/) (v2.x+)
- [Docker & Docker Compose](https://www.docker.com/)
- [Java Development Kit (JDK 21)](https://adoptium.net/)
- [Node.js (v18.x ou v20.x LTS)](https://nodejs.org/) & `npm` (v10.x+)

---

## ⚙️ Guia de Instalação e Configuração Passo a Passo

### 1. Clonar o Repositório
```bash
git clone https://github.com/seu-usuario/sistema-controle-fator-r.git
cd sistema-controle-fator-r
```

### 2. Configurar Variáveis de Ambiente
Copie o arquivo de exemplo `.env.example` para `.env`:
```bash
# Windows (PowerShell)
Copy-Item .env.example .env

# Linux / macOS
cp .env.example .env
```

---

## 🚀 Como Executar a Aplicação

### Passo 1: Subir o Banco de Dados (PostgreSQL)
Na raiz do projeto, inicie o container Docker:
```bash
docker-compose up -d
```
> O container `fatorr_db` inicializará na porta `5432` com banco `fatorr`, usuário `postgres` e senha `admin`.

### Passo 2: Executar o Backend (Spring Boot)
Abra um terminal e navegue para o diretório `backend`:
```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
> A API estará acessível em `http://localhost:8080`. Na primeira inicialização, o `DataSeeder` criará as tabelas, as faixas dos Anexos III e V, e o usuário administrador padrão:
> - **CPF:** `00000000000`
> - **Senha:** `admin123`

### Passo 3: Executar o Frontend (Angular)
Abra outro terminal e navegue para o diretório `frontend`:
```bash
cd frontend
npm install
npm start
```
> Acesse a interface web no navegador em: **`http://localhost:4200`**

---

## 🧪 Execução de Testes Automatizados

Para rodar os testes unitários do backend (regras de cálculo do Fator R e enquadramento de faixas):
```bash
cd backend

# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

---

## 📂 Estrutura de Pastas do Projeto

```plaintext
SISTEMA DE CONTROLE FATOR R/
├── .env.example                     # Template de variáveis de ambiente
├── .gitignore                       # Ignora builds, dependências e temporários
├── docker-compose.yml               # Configuração do PostgreSQL 16 em container
├── documentacao.html                # Documentação técnica estática e interativa
├── README.md                        # Guia principal do repositório
│
├── backend/                         # Módulo API REST (Spring Boot)
│   ├── mvnw / mvnw.cmd              # Wrapper do Maven
│   ├── pom.xml                      # Dependências do projeto Maven
│   └── src/
│       ├── main/
│       │   ├── java/com/fatorr/backend/
│       │   │   ├── BackendApplication.java    # Bootstrap Spring Boot
│       │   │   ├── config/DataSeeder.java     # Carga de admin e faixas padrão
│       │   │   ├── controller/                # Endpoints REST (Auth, Empresa, etc.)
│       │   │   ├── domain/                    # Entidades JPA (Empresa, Faixa, etc.)
│       │   │   ├── dto/                       # Data Transfer Objects
│       │   │   ├── repository/                # Interfaces Spring Data JPA
│       │   │   └── service/                   # Cálculos fiscais e regras de negócio
│       │   └── resources/
│       │       └── application.properties     # Configurações de banco e hibernate
│       └── test/                              # Testes automatizados (JUnit/Mockito)
│
└── frontend/                        # Módulo SPA (Angular 21)
    ├── package.json                 # Dependências Node.js / Angular
    ├── tsconfig.json                # Configurações do compilador TypeScript
    └── src/
        ├── environments/            # Configurações de API (dev/prod)
        └── app/
            ├── app.routes.ts        # Rotas da aplicação e Guards
            ├── layout/main-layout/  # Layout com header global e menu
            ├── models/models.ts     # Interfaces e tipagens TypeScript
            ├── pages/               # Views (Home, Login, Simulador, etc.)
            └── services/            # Serviços HTTP e RxJS State Context
```

---

## 📖 Documentação Técnica Detalhada

O projeto acompanha uma documentação técnica completa, estática, autocontida e com busca rápida embutida:

- **Arquivo:** [`documentacao.html`](./documentacao.html)
- **Como visualizar:** Abra o arquivo `documentacao.html` diretamente em qualquer navegador moderno (Chrome, Edge, Firefox, Safari).

---

## 📦 Instruções para Versionamento no GitHub

Para inicializar o repositório e enviar o código para o GitHub:

```bash
# 1. Inicializar o repositório Git
git init

# 2. Adicionar todos os arquivos rastreáveis (.gitignore cuidará do restante)
git add .

# 3. Criar o commit inicial
git commit -m "feat: initial commit - Sistema de Controle Fator R v1.0.0"

# 4. Renomear a branch principal para main
git branch -M main

# 5. Adicionar o repositório remoto (substitua pela URL do seu repositório)
git remote add origin https://github.com/SEU_USUARIO/sistema-controle-fator-r.git

# 6. Enviar para o GitHub
git push -u origin main
```

---

## 📄 Licença

Este projeto está sob a licença **MIT** - veja o arquivo de licença para mais detalhes.

---

## 👨‍💻 Autor & Créditos

Desenvolvido para gestão tributária e controle contábil avançado de empresas do Simples Nacional.
