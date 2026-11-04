# Dados para Criação de Contas e Testes do Sistema de Controle Fator R

> **Arquivo de Apoio para Testes Funcionais, de Integração e Carga**  
> Todos os CPFs e CNPJs contidos neste arquivo são **matematicamente válidos** (cálculo de módulo 11 da Receita Federal do Brasil), atendendo perfeitamente aos validadores do Backend (`CpfUtils.java`, `CnpjUtils.java`) e do Frontend Angular (`formatters.ts`, `cnpj.ts`).

---

## Sumário
1. [Visão Geral e Credenciais Globais](#1-visão-geral-e-credenciais-globais)
2. [Tabela Resumo de Usuários (Cheat Sheet para Copiar e Colar)](#2-tabela-resumo-de-usuários-cheat-sheet)
3. [Lista de 10 Contadores (`CONTABILISTA`)](#3-lista-de-10-contadores-contabilista)
4. [Lista de 10 Empresários (`EMPRESARIO`)](#4-lista-de-10-empresários-empresario)
5. [Lista de 10 Colaboradores (`USUARIO`)](#5-lista-de-10-colaboradores-usuario)
6. [Lista de 10 Empresas para Cadastrar (Cenários Fator R)](#6-lista-de-10-empresas-para-cadastrar-cenários-fator-r)
7. [Exemplos Prontos de Requisições de Teste (API REST / cURL)](#7-exemplos-prontos-de-requisições-de-teste-api-rest--curl)

---

## 1. Visão Geral e Credenciais Globais

- **Senha Padrão para Todos os Usuários de Teste**: `Teste@123`
- **Flexibilidade no Login**: O sistema permite autenticar informando tanto o **CPF** (com ou sem máscara) quanto o **E-mail** no campo de identificação (`identifier`).
- **Validação de Documentos**:
  - CPF: 11 dígitos numéricos com validação dos dígitos verificadores.
  - CNPJ: 14 caracteres (formato clássico ou novo formato alfanumérico RFB).
- **Regra de Negócio do Fator R**:
  $$\text{Fator R} = \frac{\text{Folha de Salários e Pró-Labore (últimos 12 meses)}}{\text{Receita Bruta Total (últimos 12 meses)}}$$
  - $\text{Fator R} \ge 28\%$: Enquadra no **Anexo III** do Simples Nacional (alíquotas mais baixas, a partir de 6%).
  - $\text{Fator R} < 28\%$: Enquadra no **Anexo V** do Simples Nacional (alíquotas maiores, a partir de 15,5%).

---

## 2. Tabela Resumo de Usuários (Cheat Sheet)

Use esta tabela para copiar e colar rapidamente as credenciais durante os testes:

| # | Perfil | Nome Completo | CPF (Formatado) | CPF (Sem Máscara) | E-mail para Login | Senha | Vínculo / Empresa Principal |
|---|---|---|---|---|---|---|---|
| 01 | **Contador** | Carlos Eduardo Moreira | `215.438.100-63` | `21543810063` | `carlos.moreira@moreiracontabil.com.br` | `Teste@123` | Moreira & Associados Assessoria Contábil |
| 02 | **Contador** | Mariana Rocha Albuquerque | `215.438.213-40` | `21543821340` | `mariana.rocha@albuquerquecont.com.br` | `Teste@123` | Albuquerque Gestão Fiscal & Contábil |
| 03 | **Contador** | Fernando Costa Silveira | `215.438.326-28` | `21543832628` | `fernando.costa@silveiracontabilidade.com.br` | `Teste@123` | Silveira Consultoria Contábil e Tributária |
| 04 | **Contador** | Luciana Fagundes Nogueira | `215.438.439-05` | `21543843905` | `luciana.fagundes@fagundescont.com.br` | `Teste@123` | Fagundes & Nogueira Contabilidade |
| 05 | **Contador** | Ricardo Mendes Peixoto | `215.438.552-45` | `21543855245` | `ricardo.mendes@mendesauditores.com.br` | `Teste@123` | Mendes Peixoto Auditores & Contadores |
| 06 | **Contador** | Camila Vasconcelos Brito | `215.438.665-22` | `21543866522` | `camila.vasconcelos@britocontabil.com.br` | `Teste@123` | Brito Gestão e Planejamento Tributário |
| 07 | **Contador** | Alexandre Prado Guimarães | `215.438.778-00` | `21543877800` | `alexandre.prado@guimaraescontabil.com.br` | `Teste@123` | Guimarães Contabilidade Corporativa |
| 08 | **Contador** | Beatriz Helena Siqueira | `215.438.891-40` | `21543889140` | `beatriz.siqueira@siqueiracont.com.br` | `Teste@123` | Siqueira Soluções Fiscais e Contábeis |
| 09 | **Contador** | Marcelo Vinicius Duarte | `215.439.004-88` | `21543900488` | `marcelo.duarte@duartecontabil.com.br` | `Teste@123` | Duarte & Cia Assessoria Contábil |
| 10 | **Contador** | Tatiane Campos Rezende | `215.439.117-65` | `21543911765` | `tatiane.rezende@rezendecontabilidade.com.br` | `Teste@123` | Rezende Inteligência Contábil |
| 11 | **Empresário** | Bruno Henrique Castro | `215.439.230-03` | `21543923003` | `bruno.castro@inovarsoftware.com.br` | `Teste@123` | Inovar Softwares |
| 12 | **Empresário** | Patrícia Mendes Lima | `215.439.343-82` | `21543934382` | `patricia.lima@clinicaortovita.com.br` | `Teste@123` | Clínica Ortovita |
| 13 | **Empresário** | Rodrigo Alves Nogueira | `215.439.456-60` | `21543945660` | `rodrigo.nogueira@nexusedu.com.br` | `Teste@123` | Nexus Educação Corporativa |
| 14 | **Empresário** | Juliana Barbosa Paiva | `215.439.569-47` | `21543956947` | `juliana.paiva@arqurban.com.br` | `Teste@123` | Studio ArqUrban |
| 15 | **Empresário** | Gustavo Henrique Toledo | `215.439.682-87` | `21543968287` | `gustavo.toledo@vertexdigital.com.br` | `Teste@123` | Vertex Digital Agency |
| 16 | **Empresário** | Renata Carvalho Pires | `215.439.795-64` | `21543979564` | `renata.pires@dentalsmilecare.com.br` | `Teste@123` | Dental Smile Care |
| 17 | **Empresário** | Felipe Antunes Barreto | `215.439.908-86` | `21543990886` | `felipe.barreto@solucoesiot.com.br` | `Teste@123` | IoT Core Automação |
| 18 | **Empresário** | Aline Vasconcelos Farias | `215.440.021-30` | `21544002130` | `aline.farias@periciasengenharia.com.br` | `Teste@123` | Farias Perícias & Engenharia |
| 19 | **Empresário** | Thiago Mendonça Leal | `215.440.134-18` | `21544013418` | `thiago.leal@dataprimeanalytics.com.br` | `Teste@123` | Data Prime Analytics |
| 20 | **Empresário** | Vanessa Couto Marinho | `215.440.247-03` | `21544024703` | `vanessa.marinho@clinicaanimavita.com.br` | `Teste@123` | Centro Veterinário AnimaVita |
| 21 | **Colaborador** | Lucas Gabriel Fonseca | `215.440.360-35` | `21544036035` | `lucas.fonseca@inovarsoftware.com.br` | `Teste@123` | Inovar Tecnologia (Analista Financeiro) |
| 22 | **Colaborador** | Juliana Martins Ribeiro | `215.440.473-12` | `21544047312` | `juliana.ribeiro@clinicaortovita.com.br` | `Teste@123` | Clínica Ortovita (Assistente Faturamento) |
| 23 | **Colaborador** | Thiago Siqueira Prado | `215.440.586-08` | `21544058608` | `thiago.prado@nexusedu.com.br` | `Teste@123` | Nexus Educação (Coord. Administrativo) |
| 24 | **Colaborador** | Larissa Nogueira Sales | `215.440.699-87` | `21544069987` | `larissa.sales@arqurban.com.br` | `Teste@123` | Studio ArqUrban (Arquiteta Projetista) |
| 25 | **Colaborador** | Gabriel Monteiro Bastos | `215.440.812-51` | `21544081251` | `gabriel.bastos@vertexdigital.com.br` | `Teste@123` | Vertex Digital (Especialista Mídia) |
| 26 | **Colaborador** | Priscila Dorneles Ramos | `215.440.925-39` | `21544092539` | `priscila.ramos@dentalsmilecare.com.br` | `Teste@123` | Dental Smile (Recepcionista) |
| 27 | **Colaborador** | Matheus Henrique Vieira | `215.441.038-32` | `21544103832` | `matheus.vieira@solucoesiot.com.br` | `Teste@123` | Soluções IoT (Dev Embarcado) |
| 28 | **Colaborador** | Camila Silveira Fontes | `215.441.151-72` | `21544115172` | `camila.fontes@periciasengenharia.com.br` | `Teste@123` | Farias Perícias (Assistente Técnica) |
| 29 | **Colaborador** | Daniel Correia Guedes | `215.441.264-50` | `21544126450` | `daniel.guedes@dataprimeanalytics.com.br` | `Teste@123` | Data Prime (Engenheiro de Dados Jr) |
| 30 | **Colaborador** | Fernanda Tavares Paes | `215.441.377-37` | `21544137737` | `fernanda.paes@clinicaanimavita.com.br` | `Teste@123` | AnimaVita (Assistente de DP) |

---

## 3. Lista de 10 Contadores (`CONTABILISTA`)

Usuários que exercem a função de **Administrador Contábil** (`TipoUsuario.ADMINISTRADOR_CONTABIL` / `TipoPerfil.CONTABILISTA`). Têm poder de gestão sobre a Organização Contábil, empresas clientes, parametrização de alíquotas do Simples Nacional, laudos e convite de usuários.

### Contador 01 - Carlos Eduardo Moreira
- **Nome Completo**: Carlos Eduardo Moreira
- **E-mail**: `carlos.moreira@moreiracontabil.com.br`
- **CPF Formatado**: `215.438.100-63`
- **CPF Sem Pontuação**: `21543810063`
- **Senha**: `Teste@123`
- **Organização Contábil**: Moreira & Associados Assessoria Contábil Ltda
- **CNPJ da Organização**: `28.145.920/0001-17` (limpo: `28145920000117`)
- **Registro CRC**: `CRC-SP 128941/O`
- **Telefone**: `(11) 98765-1001`
- **Localidade**: São Paulo / SP

```json
{
  "nome": "Carlos Eduardo Moreira",
  "email": "carlos.moreira@moreiracontabil.com.br",
  "cpf": "21543810063",
  "senha": "Teste@123",
  "nomeOrganizacao": "Moreira & Associados Assessoria Contábil Ltda",
  "cnpj": "28145920000117",
  "crc": "CRC-SP 128941/O"
}
```

### Contador 02 - Mariana Rocha Albuquerque
- **Nome Completo**: Mariana Rocha Albuquerque
- **E-mail**: `mariana.rocha@albuquerquecont.com.br`
- **CPF Formatado**: `215.438.213-40`
- **CPF Sem Pontuação**: `21543821340`
- **Senha**: `Teste@123`
- **Organização Contábil**: Albuquerque Gestão Fiscal & Contábil Ltda
- **CNPJ da Organização**: `28.145.937/0001-74` (limpo: `28145937000174`)
- **Registro CRC**: `CRC-RJ 094512/O`
- **Telefone**: `(21) 98123-2002`
- **Localidade**: Rio de Janeiro / RJ

```json
{
  "nome": "Mariana Rocha Albuquerque",
  "email": "mariana.rocha@albuquerquecont.com.br",
  "cpf": "21543821340",
  "senha": "Teste@123",
  "nomeOrganizacao": "Albuquerque Gestão Fiscal & Contábil Ltda",
  "cnpj": "28145937000174",
  "crc": "CRC-RJ 094512/O"
}
```

### Contador 03 - Fernando Costa Silveira
- **Nome Completo**: Fernando Costa Silveira
- **E-mail**: `fernando.costa@silveiracontabilidade.com.br`
- **CPF Formatado**: `215.438.326-28`
- **CPF Sem Pontuação**: `21543832628`
- **Senha**: `Teste@123`
- **Organização Contábil**: Silveira Consultoria Contábil e Tributária Ltda
- **CNPJ da Organização**: `28.145.954/0001-01` (limpo: `28145954000101`)
- **Registro CRC**: `CRC-MG 078345/O`
- **Telefone**: `(31) 98456-3003`
- **Localidade**: Belo Horizonte / MG

```json
{
  "nome": "Fernando Costa Silveira",
  "email": "fernando.costa@silveiracontabilidade.com.br",
  "cpf": "21543832628",
  "senha": "Teste@123",
  "nomeOrganizacao": "Silveira Consultoria Contábil e Tributária Ltda",
  "cnpj": "28145954000101",
  "crc": "CRC-MG 078345/O"
}
```

### Contador 04 - Luciana Fagundes Nogueira
- **Nome Completo**: Luciana Fagundes Nogueira
- **E-mail**: `luciana.fagundes@fagundescont.com.br`
- **CPF Formatado**: `215.438.439-05`
- **CPF Sem Pontuação**: `21543843905`
- **Senha**: `Teste@123`
- **Organização Contábil**: Fagundes & Nogueira Contabilidade Estratégica Ltda
- **CNPJ da Organização**: `28.145.971/0001-49` (limpo: `28145971000149`)
- **Registro CRC**: `CRC-PR 061278/O`
- **Telefone**: `(41) 99123-4004`
- **Localidade**: Curitiba / PR

```json
{
  "nome": "Luciana Fagundes Nogueira",
  "email": "luciana.fagundes@fagundescont.com.br",
  "cpf": "21543843905",
  "senha": "Teste@123",
  "nomeOrganizacao": "Fagundes & Nogueira Contabilidade Estratégica Ltda",
  "cnpj": "28145971000149",
  "crc": "CRC-PR 061278/O"
}
```

### Contador 05 - Ricardo Mendes Peixoto
- **Nome Completo**: Ricardo Mendes Peixoto
- **E-mail**: `ricardo.mendes@mendesauditores.com.br`
- **CPF Formatado**: `215.438.552-45`
- **CPF Sem Pontuação**: `21543855245`
- **Senha**: `Teste@123`
- **Organização Contábil**: Mendes Peixoto Auditores & Contadores Associados Ltda
- **CNPJ da Organização**: `28.145.988/0001-04` (limpo: `28145988000104`)
- **Registro CRC**: `CRC-RS 053912/O`
- **Telefone**: `(51) 99345-5005`
- **Localidade**: Porto Alegre / RS

```json
{
  "nome": "Ricardo Mendes Peixoto",
  "email": "ricardo.mendes@mendesauditores.com.br",
  "cpf": "21543855245",
  "senha": "Teste@123",
  "nomeOrganizacao": "Mendes Peixoto Auditores & Contadores Associados Ltda",
  "cnpj": "28145988000104",
  "crc": "CRC-RS 053912/O"
}
```

### Contador 06 - Camila Vasconcelos Brito
- **Nome Completo**: Camila Vasconcelos Brito
- **E-mail**: `camila.vasconcelos@britocontabil.com.br`
- **CPF Formatado**: `215.438.665-22`
- **CPF Sem Pontuação**: `21543866522`
- **Senha**: `Teste@123`
- **Organização Contábil**: Brito Gestão e Planejamento Tributário Ltda
- **CNPJ da Organização**: `28.146.005/0001-46` (limpo: `28146005000146`)
- **Registro CRC**: `CRC-SC 049811/O`
- **Telefone**: `(48) 98877-6006`
- **Localidade**: Florianópolis / SC

```json
{
  "nome": "Camila Vasconcelos Brito",
  "email": "camila.vasconcelos@britocontabil.com.br",
  "cpf": "21543866522",
  "senha": "Teste@123",
  "nomeOrganizacao": "Brito Gestão e Planejamento Tributário Ltda",
  "cnpj": "28146005000146",
  "crc": "CRC-SC 049811/O"
}
```

### Contador 07 - Alexandre Prado Guimarães
- **Nome Completo**: Alexandre Prado Guimarães
- **E-mail**: `alexandre.prado@guimaraescontabil.com.br`
- **CPF Formatado**: `215.438.778-00`
- **CPF Sem Pontuação**: `21543877800`
- **Senha**: `Teste@123`
- **Organização Contábil**: Guimarães Contabilidade Corporativa Ltda
- **CNPJ da Organização**: `28.146.022/0001-83` (limpo: `28146022000183`)
- **Registro CRC**: `CRC-BA 037645/O`
- **Telefone**: `(71) 98234-7007`
- **Localidade**: Salvador / BA

```json
{
  "nome": "Alexandre Prado Guimarães",
  "email": "alexandre.prado@guimaraescontabil.com.br",
  "cpf": "21543877800",
  "senha": "Teste@123",
  "nomeOrganizacao": "Guimarães Contabilidade Corporativa Ltda",
  "cnpj": "28146022000183",
  "crc": "CRC-BA 037645/O"
}
```

### Contador 08 - Beatriz Helena Siqueira
- **Nome Completo**: Beatriz Helena Siqueira
- **E-mail**: `beatriz.siqueira@siqueiracont.com.br`
- **CPF Formatado**: `215.438.891-40`
- **CPF Sem Pontuação**: `21543889140`
- **Senha**: `Teste@123`
- **Organização Contábil**: Siqueira Soluções Fiscais e Contábeis Ltda
- **CNPJ da Organização**: `28.146.039/0001-30` (limpo: `28146039000130`)
- **Registro CRC**: `CRC-DF 025490/O`
- **Telefone**: `(61) 99654-8008`
- **Localidade**: Brasília / DF

```json
{
  "nome": "Beatriz Helena Siqueira",
  "email": "beatriz.siqueira@siqueiracont.com.br",
  "cpf": "21543889140",
  "senha": "Teste@123",
  "nomeOrganizacao": "Siqueira Soluções Fiscais e Contábeis Ltda",
  "cnpj": "28146039000130",
  "crc": "CRC-DF 025490/O"
}
```

### Contador 09 - Marcelo Vinicius Duarte
- **Nome Completo**: Marcelo Vinicius Duarte
- **E-mail**: `marcelo.duarte@duartecontabil.com.br`
- **CPF Formatado**: `215.439.004-88`
- **CPF Sem Pontuação**: `21543900488`
- **Senha**: `Teste@123`
- **Organização Contábil**: Duarte & Cia Assessoria Contábil Ltda
- **CNPJ da Organização**: `28.146.056/0001-78` (limpo: `28146056000178`)
- **Registro CRC**: `CRC-GO 031890/O`
- **Telefone**: `(62) 98541-9009`
- **Localidade**: Goiânia / GO

```json
{
  "nome": "Marcelo Vinicius Duarte",
  "email": "marcelo.duarte@duartecontabil.com.br",
  "cpf": "21543900488",
  "senha": "Teste@123",
  "nomeOrganizacao": "Duarte & Cia Assessoria Contábil Ltda",
  "cnpj": "28146056000178",
  "crc": "CRC-GO 031890/O"
}
```

### Contador 10 - Tatiane Campos Rezende
- **Nome Completo**: Tatiane Campos Rezende
- **E-mail**: `tatiane.rezende@rezendecontabilidade.com.br`
- **CPF Formatado**: `215.439.117-65`
- **CPF Sem Pontuação**: `21543911765`
- **Senha**: `Teste@123`
- **Organização Contábil**: Rezende Inteligência Contábil Ltda
- **CNPJ da Organização**: `28.146.073/0001-05` (limpo: `28146073000105`)
- **Registro CRC**: `CRC-PE 029415/O`
- **Telefone**: `(81) 99182-1010`
- **Localidade**: Recife / PE

```json
{
  "nome": "Tatiane Campos Rezende",
  "email": "tatiane.rezende@rezendecontabilidade.com.br",
  "cpf": "21543911765",
  "senha": "Teste@123",
  "nomeOrganizacao": "Rezende Inteligência Contábil Ltda",
  "cnpj": "28146073000105",
  "crc": "CRC-PE 029415/O"
}
```

---

## 4. Lista de 10 Empresários (`EMPRESARIO`)

Usuários donos ou sócios de empresas (`TipoUsuario.USUARIO_EMPRESA` / `TipoPerfil.EMPRESARIO`). Possuem acesso gerencial sobre sua empresa, dashboards financeiros, projeção de folha/pró-labore e histórico de Fator R.

### Empresário 01 - Bruno Henrique Castro
- **Nome Completo**: Bruno Henrique Castro
- **E-mail**: `bruno.castro@inovarsoftware.com.br`
- **CPF Formatado**: `215.439.230-03`
- **CPF Sem Pontuação**: `21543923003`
- **Senha**: `Teste@123`
- **Empresa**: Inovar Tecnologia e Desenvolvimento de Software Ltda
- **Nome Fantasia**: Inovar Softwares
- **CNPJ da Empresa**: `28.146.090/0001-42` (limpo: `28146090000142`)
- **Cargo / Função**: Sócio Administrador
- **Pró-Labore Almejado**: R$ 12.000,00
- **Telefone**: `(11) 97123-1111`

```json
{
  "nome": "Bruno Henrique Castro",
  "email": "bruno.castro@inovarsoftware.com.br",
  "cpf": "21543923003",
  "senha": "Teste@123",
  "razaoSocial": "Inovar Tecnologia e Desenvolvimento de Software Ltda",
  "nomeFantasia": "Inovar Softwares",
  "cnpj": "28146090000142"
}
```

### Empresário 02 - Patrícia Mendes Lima
- **Nome Completo**: Patrícia Mendes Lima
- **E-mail**: `patricia.lima@clinicaortovita.com.br`
- **CPF Formatado**: `215.439.343-82`
- **CPF Sem Pontuação**: `21543934382`
- **Senha**: `Teste@123`
- **Empresa**: Clínica Médica e Fisioterapia Ortovita Ltda
- **Nome Fantasia**: Clínica Ortovita
- **CNPJ da Empresa**: `28.146.107/0001-61` (limpo: `28146107000161`)
- **Cargo / Função**: Sócia Diretora
- **Pró-Labore Almejado**: R$ 15.000,00
- **Telefone**: `(21) 97234-2222`

```json
{
  "nome": "Patrícia Mendes Lima",
  "email": "patricia.lima@clinicaortovita.com.br",
  "cpf": "21543934382",
  "senha": "Teste@123",
  "razaoSocial": "Clínica Médica e Fisioterapia Ortovita Ltda",
  "nomeFantasia": "Clínica Ortovita",
  "cnpj": "28146107000161"
}
```

### Empresário 03 - Rodrigo Alves Nogueira
- **Nome Completo**: Rodrigo Alves Nogueira
- **E-mail**: `rodrigo.nogueira@nexusedu.com.br`
- **CPF Formatado**: `215.439.456-60`
- **CPF Sem Pontuação**: `21543945660`
- **Senha**: `Teste@123`
- **Empresa**: Nexus Treinamentos e Consultoria Empresarial Ltda
- **Nome Fantasia**: Nexus Educação Corporativa
- **CNPJ da Empresa**: `28.146.124/0001-07` (limpo: `28146124000107`)
- **Cargo / Função**: Sócio Administrador
- **Pró-Labore Almejado**: R$ 8.500,00
- **Telefone**: `(31) 97345-3333`

```json
{
  "nome": "Rodrigo Alves Nogueira",
  "email": "rodrigo.nogueira@nexusedu.com.br",
  "cpf": "21543945660",
  "senha": "Teste@123",
  "razaoSocial": "Nexus Treinamentos e Consultoria Empresarial Ltda",
  "nomeFantasia": "Nexus Educação Corporativa",
  "cnpj": "28146124000107"
}
```

### Empresário 04 - Juliana Barbosa Paiva
- **Nome Completo**: Juliana Barbosa Paiva
- **E-mail**: `juliana.paiva@arqurban.com.br`
- **CPF Formatado**: `215.439.569-47`
- **CPF Sem Pontuação**: `21543956947`
- **Senha**: `Teste@123`
- **Empresa**: Studio ArqUrban Arquitetura e Engenharia Ltda
- **Nome Fantasia**: Studio ArqUrban
- **CNPJ da Empresa**: `28.146.141/0001-36` (limpo: `28146141000136`)
- **Cargo / Função**: Sócia Gerente
- **Pró-Labore Almejado**: R$ 10.000,00
- **Telefone**: `(41) 97456-4444`

```json
{
  "nome": "Juliana Barbosa Paiva",
  "email": "juliana.paiva@arqurban.com.br",
  "cpf": "21543956947",
  "senha": "Teste@123",
  "razaoSocial": "Studio ArqUrban Arquitetura e Engenharia Ltda",
  "nomeFantasia": "Studio ArqUrban",
  "cnpj": "28146141000136"
}
```

### Empresário 05 - Gustavo Henrique Toledo
- **Nome Completo**: Gustavo Henrique Toledo
- **E-mail**: `gustavo.toledo@vertexdigital.com.br`
- **CPF Formatado**: `215.439.682-87`
- **CPF Sem Pontuação**: `21543968287`
- **Senha**: `Teste@123`
- **Empresa**: Vertex Agência de Marketing Digital & Estratégia Ltda
- **Nome Fantasia**: Vertex Digital Agency
- **CNPJ da Empresa**: `28.146.158/0001-93` (limpo: `28146158000193`)
- **Cargo / Função**: Sócio Administrador
- **Pró-Labore Almejado**: R$ 9.000,00
- **Telefone**: `(51) 97567-5555`

```json
{
  "nome": "Gustavo Henrique Toledo",
  "email": "gustavo.toledo@vertexdigital.com.br",
  "cpf": "21543968287",
  "senha": "Teste@123",
  "razaoSocial": "Vertex Agência de Marketing Digital & Estratégia Ltda",
  "nomeFantasia": "Vertex Digital Agency",
  "cnpj": "28146158000193"
}
```

### Empresário 06 - Renata Carvalho Pires
- **Nome Completo**: Renata Carvalho Pires
- **E-mail**: `renata.pires@dentalsmilecare.com.br`
- **CPF Formatado**: `215.439.795-64`
- **CPF Sem Pontuação**: `21543979564`
- **Senha**: `Teste@123`
- **Empresa**: Dental Smile Care Odontologia Especializada Ltda
- **Nome Fantasia**: Dental Smile Care
- **CNPJ da Empresa**: `28.146.175/0001-20` (limpo: `28146175000120`)
- **Cargo / Função**: Sócia Administradora
- **Pró-Labore Almejado**: R$ 14.000,00
- **Telefone**: `(48) 97678-6666`

```json
{
  "nome": "Renata Carvalho Pires",
  "email": "renata.pires@dentalsmilecare.com.br",
  "cpf": "21543979564",
  "senha": "Teste@123",
  "razaoSocial": "Dental Smile Care Odontologia Especializada Ltda",
  "nomeFantasia": "Dental Smile Care",
  "cnpj": "28146175000120"
}
```

### Empresário 07 - Felipe Antunes Barreto
- **Nome Completo**: Felipe Antunes Barreto
- **E-mail**: `felipe.barreto@solucoesiot.com.br`
- **CPF Formatado**: `215.439.908-86`
- **CPF Sem Pontuação**: `21543990886`
- **Senha**: `Teste@123`
- **Empresa**: Soluções IoT e Automação Industrial Ltda
- **Nome Fantasia**: IoT Core Automação
- **CNPJ da Empresa**: `28.146.192/0001-68` (limpo: `28146192000168`)
- **Cargo / Função**: Diretor Executivo (CEO)
- **Pró-Labore Almejado**: R$ 13.500,00
- **Telefone**: `(71) 97789-7777`

```json
{
  "nome": "Felipe Antunes Barreto",
  "email": "felipe.barreto@solucoesiot.com.br",
  "cpf": "21543990886",
  "senha": "Teste@123",
  "razaoSocial": "Soluções IoT e Automação Industrial Ltda",
  "nomeFantasia": "IoT Core Automação",
  "cnpj": "28146192000168"
}
```

### Empresário 08 - Aline Vasconcelos Farias
- **Nome Completo**: Aline Vasconcelos Farias
- **E-mail**: `aline.farias@periciasengenharia.com.br`
- **CPF Formatado**: `215.440.021-30`
- **CPF Sem Pontuação**: `21544002130`
- **Senha**: `Teste@123`
- **Empresa**: Farias Perícias Judiciais e Avaliações Técnicas Ltda
- **Nome Fantasia**: Farias Perícias & Engenharia
- **CNPJ da Empresa**: `28.146.209/0001-87` (limpo: `28146209000187`)
- **Cargo / Função**: Sócia Titular
- **Pró-Labore Almejado**: R$ 11.000,00
- **Telefone**: `(61) 97890-8888`

```json
{
  "nome": "Aline Vasconcelos Farias",
  "email": "aline.farias@periciasengenharia.com.br",
  "cpf": "21544002130",
  "senha": "Teste@123",
  "razaoSocial": "Farias Perícias Judiciais e Avaliações Técnicas Ltda",
  "nomeFantasia": "Farias Perícias & Engenharia",
  "cnpj": "28146209000187"
}
```

### Empresário 09 - Thiago Mendonça Leal
- **Nome Completo**: Thiago Mendonça Leal
- **E-mail**: `thiago.leal@dataprimeanalytics.com.br`
- **CPF Formatado**: `215.440.134-18`
- **CPF Sem Pontuação**: `21544013418`
- **Senha**: `Teste@123`
- **Empresa**: Data Prime Ciência de Dados e IA Ltda
- **Nome Fantasia**: Data Prime Analytics
- **CNPJ da Empresa**: `28.146.226/0001-14` (limpo: `28146226000114`)
- **Cargo / Função**: Sócio Administrador
- **Pró-Labore Almejado**: R$ 16.000,00
- **Telefone**: `(62) 97901-9999`

```json
{
  "nome": "Thiago Mendonça Leal",
  "email": "thiago.leal@dataprimeanalytics.com.br",
  "cpf": "21544013418",
  "senha": "Teste@123",
  "razaoSocial": "Data Prime Ciência de Dados e IA Ltda",
  "nomeFantasia": "Data Prime Analytics",
  "cnpj": "28146226000114"
}
```

### Empresário 10 - Vanessa Couto Marinho
- **Nome Completo**: Vanessa Couto Marinho
- **E-mail**: `vanessa.marinho@clinicaanimavita.com.br`
- **CPF Formatado**: `215.440.247-03`
- **CPF Sem Pontuação**: `21544024703`
- **Senha**: `Teste@123`
- **Empresa**: Clínica Veterinária e Diagnóstico AnimaVita Ltda
- **Nome Fantasia**: Centro Veterinário AnimaVita
- **CNPJ da Empresa**: `28.146.243/0001-51` (limpo: `28146243000151`)
- **Cargo / Função**: Sócia Administradora
- **Pró-Labore Almejado**: R$ 9.500,00
- **Telefone**: `(81) 98012-0000`

```json
{
  "nome": "Vanessa Couto Marinho",
  "email": "vanessa.marinho@clinicaanimavita.com.br",
  "cpf": "21544024703",
  "senha": "Teste@123",
  "razaoSocial": "Clínica Veterinária e Diagnóstico AnimaVita Ltda",
  "nomeFantasia": "Centro Veterinário AnimaVita",
  "cnpj": "28146243000151"
}
```

---

## 5. Lista de 10 Colaboradores (`USUARIO`)

Usuários operacionais da empresa (`TipoUsuario.USUARIO_EMPRESA` / `TipoPerfil.USUARIO`). Executam lançamento de extratos bancários, conciliações, contas a receber e conferência de folha de pagamento.

### Colaborador 01 - Lucas Gabriel Fonseca
- **Nome Completo**: Lucas Gabriel Fonseca
- **E-mail**: `lucas.fonseca@inovarsoftware.com.br`
- **CPF Formatado**: `215.440.360-35`
- **CPF Sem Pontuação**: `21544036035`
- **Senha**: `Teste@123`
- **Cargo / Função**: Analista Financeiro Pleno
- **Empresa Vinculada**: Inovar Tecnologia e Desenvolvimento de Software Ltda
- **Salário Base Estimado**: R$ 4.200,00
- **Telefone**: `(11) 96541-1001`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Lucas Gabriel Fonseca",
  "email": "lucas.fonseca@inovarsoftware.com.br",
  "cpf": "21544036035",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_INOVAR>",
  "funcao": "Analista Financeiro Pleno"
}
```

### Colaborador 02 - Juliana Martins Ribeiro
- **Nome Completo**: Juliana Martins Ribeiro
- **E-mail**: `juliana.ribeiro@clinicaortovita.com.br`
- **CPF Formatado**: `215.440.473-12`
- **CPF Sem Pontuação**: `21544047312`
- **Senha**: `Teste@123`
- **Cargo / Função**: Assistente de Faturamento Hospitalar
- **Empresa Vinculada**: Clínica Médica e Fisioterapia Ortovita Ltda
- **Salário Base Estimado**: R$ 3.400,00
- **Telefone**: `(21) 96652-2002`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Juliana Martins Ribeiro",
  "email": "juliana.ribeiro@clinicaortovita.com.br",
  "cpf": "21544047312",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_ORTOVITA>",
  "funcao": "Assistente de Faturamento Hospitalar"
}
```

### Colaborador 03 - Thiago Siqueira Prado
- **Nome Completo**: Thiago Siqueira Prado
- **E-mail**: `thiago.prado@nexusedu.com.br`
- **CPF Formatado**: `215.440.586-08`
- **CPF Sem Pontuação**: `21544058608`
- **Senha**: `Teste@123`
- **Cargo / Função**: Coordenador Administrativo
- **Empresa Vinculada**: Nexus Treinamentos e Consultoria Empresarial Ltda
- **Salário Base Estimado**: R$ 5.100,00
- **Telefone**: `(31) 96763-3003`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Thiago Siqueira Prado",
  "email": "thiago.prado@nexusedu.com.br",
  "cpf": "21544058608",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_NEXUS>",
  "funcao": "Coordenador Administrativo"
}
```

### Colaborador 04 - Larissa Nogueira Sales
- **Nome Completo**: Larissa Nogueira Sales
- **E-mail**: `larissa.sales@arqurban.com.br`
- **CPF Formatado**: `215.440.699-87`
- **CPF Sem Pontuação**: `21544069987`
- **Senha**: `Teste@123`
- **Cargo / Função**: Arquiteta Projetista Pleno
- **Empresa Vinculada**: Studio ArqUrban Arquitetura e Engenharia Ltda
- **Salário Base Estimado**: R$ 6.300,00
- **Telefone**: `(41) 96874-4004`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Larissa Nogueira Sales",
  "email": "larissa.sales@arqurban.com.br",
  "cpf": "21544069987",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_ARQURBAN>",
  "funcao": "Arquiteta Projetista Pleno"
}
```

### Colaborador 05 - Gabriel Monteiro Bastos
- **Nome Completo**: Gabriel Monteiro Bastos
- **E-mail**: `gabriel.bastos@vertexdigital.com.br`
- **CPF Formatado**: `215.440.812-51`
- **CPF Sem Pontuação**: `21544081251`
- **Senha**: `Teste@123`
- **Cargo / Função**: Especialista em Mídia e Tráfego Pago
- **Empresa Vinculada**: Vertex Agência de Marketing Digital & Estratégia Ltda
- **Salário Base Estimado**: R$ 4.800,00
- **Telefone**: `(51) 96985-5005`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Gabriel Monteiro Bastos",
  "email": "gabriel.bastos@vertexdigital.com.br",
  "cpf": "21544081251",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_VERTEX>",
  "funcao": "Especialista em Mídia e Tráfego Pago"
}
```

### Colaborador 06 - Priscila Dorneles Ramos
- **Nome Completo**: Priscila Dorneles Ramos
- **E-mail**: `priscila.ramos@dentalsmilecare.com.br`
- **CPF Formatado**: `215.440.925-39`
- **CPF Sem Pontuação**: `21544092539`
- **Senha**: `Teste@123`
- **Cargo / Função**: Recepcionista e Concierge Odontológico
- **Empresa Vinculada**: Dental Smile Care Odontologia Especializada Ltda
- **Salário Base Estimado**: R$ 2.900,00
- **Telefone**: `(48) 96096-6006`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Priscila Dorneles Ramos",
  "email": "priscila.ramos@dentalsmilecare.com.br",
  "cpf": "21544092539",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_DENTALSMILE>",
  "funcao": "Recepcionista e Concierge Odontológico"
}
```

### Colaborador 07 - Matheus Henrique Vieira
- **Nome Completo**: Matheus Henrique Vieira
- **E-mail**: `matheus.vieira@solucoesiot.com.br`
- **CPF Formatado**: `215.441.038-32`
- **CPF Sem Pontuação**: `21544103832`
- **Senha**: `Teste@123`
- **Cargo / Função**: Desenvolvedor de Software Embarcado
- **Empresa Vinculada**: Soluções IoT e Automação Industrial Ltda
- **Salário Base Estimado**: R$ 7.500,00
- **Telefone**: `(71) 96107-7007`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Matheus Henrique Vieira",
  "email": "matheus.vieira@solucoesiot.com.br",
  "cpf": "21544103832",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_IOT>",
  "funcao": "Desenvolvedor de Software Embarcado"
}
```

### Colaborador 08 - Camila Silveira Fontes
- **Nome Completo**: Camila Silveira Fontes
- **E-mail**: `camila.fontes@periciasengenharia.com.br`
- **CPF Formatado**: `215.441.151-72`
- **CPF Sem Pontuação**: `21544115172`
- **Senha**: `Teste@123`
- **Cargo / Função**: Assistente Técnica de Laudos e Perícias
- **Empresa Vinculada**: Farias Perícias Judiciais e Avaliações Técnicas Ltda
- **Salário Base Estimado**: R$ 3.800,00
- **Telefone**: `(61) 96218-8008`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Camila Silveira Fontes",
  "email": "camila.fontes@periciasengenharia.com.br",
  "cpf": "21544115172",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_FARIAS>",
  "funcao": "Assistente Técnica de Laudos e Perícias"
}
```

### Colaborador 09 - Daniel Correia Guedes
- **Nome Completo**: Daniel Correia Guedes
- **E-mail**: `daniel.guedes@dataprimeanalytics.com.br`
- **CPF Formatado**: `215.441.264-50`
- **CPF Sem Pontuação**: `21544126450`
- **Senha**: `Teste@123`
- **Cargo / Função**: Engenheiro de Dados Jr
- **Empresa Vinculada**: Data Prime Ciência de Dados e IA Ltda
- **Salário Base Estimado**: R$ 5.600,00
- **Telefone**: `(62) 96329-9009`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Daniel Correia Guedes",
  "email": "daniel.guedes@dataprimeanalytics.com.br",
  "cpf": "21544126450",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_DATAPRIME>",
  "funcao": "Engenheiro de Dados Jr"
}
```

### Colaborador 10 - Fernanda Tavares Paes
- **Nome Completo**: Fernanda Tavares Paes
- **E-mail**: `fernanda.paes@clinicaanimavita.com.br`
- **CPF Formatado**: `215.441.377-37`
- **CPF Sem Pontuação**: `21544137737`
- **Senha**: `Teste@123`
- **Cargo / Função**: Assistente de Departamento Pessoal
- **Empresa Vinculada**: Clínica Veterinária e Diagnóstico AnimaVita Ltda
- **Salário Base Estimado**: R$ 3.200,00
- **Telefone**: `(81) 96430-1010`
- **Perfil / Status**: `USUARIO` / `StatusVinculo.APROVADO`

```json
{
  "nome": "Fernanda Tavares Paes",
  "email": "fernanda.paes@clinicaanimavita.com.br",
  "cpf": "21544137737",
  "senha": "Teste@123",
  "empresaId": "<ID_OU_UUID_EMPRESA_ANIMAVITA>",
  "funcao": "Assistente de Departamento Pessoal"
}
```

---

## 6. Lista de 10 Empresas para Cadastrar (Cenários Fator R)

As empresas abaixo atuam em atividades intelectuais e de serviços regulamentadas pelo **Art. 18, § 5º-J da Lei Complementar nº 123/2006**, onde a relação entre folha de salários e receita bruta define se a alíquota é recolhida pelo **Anexo III** (mais vantajoso) ou pelo **Anexo V** (mais oneroso).

### Tabela Resumo das Empresas

| # | Razão Social | Nome Fantasia | CNPJ (Formatado) | CNPJ (Sem Máscara) | Início Ativ. | RBT12 (Ano) | Folha12 (Ano) | Fator R % | Anexo Resultante |
|---|---|---|---|---|---|---|---|---|---|
| 01 | Inovar Tecnologia e Desenv. de Software Ltda | Inovar Softwares | `28.146.090/0001-42` | `28146090000142` | `2023-01-15` | R$ 480.000,00 | R$ 156.000,00 | **32,50%** | **Anexo III** (Econômico) |
| 02 | Clínica Médica e Fisioterapia Ortovita Ltda | Clínica Ortovita | `28.146.107/0001-61` | `28146107000161` | `2022-05-10` | R$ 720.000,00 | R$ 225.000,00 | **31,25%** | **Anexo III** (Econômico) |
| 03 | Nexus Treinamentos e Consultoria Empresarial Ltda | Nexus Educação | `28.146.124/0001-07` | `28146124000107` | `2023-08-20` | R$ 600.000,00 | R$ 120.000,00 | **20,00%** | **Anexo V** (Otimizável) |
| 04 | Studio ArqUrban Arquitetura e Engenharia Ltda | Studio ArqUrban | `28.146.141/0001-36` | `28146141000136` | `2021-11-01` | R$ 360.000,00 | R$ 108.000,00 | **30,00%** | **Anexo III** (Econômico) |
| 05 | Vertex Agência de Marketing Digital & Estratégia Ltda | Vertex Digital | `28.146.158/0001-93` | `28146158000193` | `2024-02-15` | R$ 300.000,00 | R$ 66.000,00 | **22,00%** | **Anexo V** (Otimizável) |
| 06 | Dental Smile Care Odontologia Especializada Ltda | Dental Smile | `28.146.175/0001-20` | `28146175000120` | `2022-03-01` | R$ 540.000,00 | R$ 180.000,00 | **33,33%** | **Anexo III** (Econômico) |
| 07 | Soluções IoT e Automação Industrial Ltda | IoT Core | `28.146.192/0001-68` | `28146192000168` | `2023-04-12` | R$ 960.000,00 | R$ 310.000,00 | **32,29%** | **Anexo III** (Econômico) |
| 08 | Farias Perícias Judiciais e Avaliações Técnicas Ltda | Farias Perícias | `28.146.209/0001-87` | `28146209000187` | `2021-07-25` | R$ 420.000,00 | R$ 92.400,00 | **22,00%** | **Anexo V** (Otimizável) |
| 09 | Data Prime Ciência de Dados e IA Ltda | Data Prime | `28.146.226/0001-14` | `28146226000114` | `2024-01-10` | R$ 1.200.000,00 | R$ 420.000,00 | **35,00%** | **Anexo III** (Econômico) |
| 10 | Clínica Veterinária e Diagnóstico AnimaVita Ltda | Centro AnimaVita | `28.146.243/0001-51` | `28146243000151` | `2022-09-01` | R$ 380.000,00 | R$ 114.000,00 | **30,00%** | **Anexo III** (Econômico) |

---

### Fichas Detalhadas de Cada Empresa

#### Empresa 01 - Inovar Tecnologia e Desenvolvimento de Software Ltda
- **Razão Social**: `Inovar Tecnologia e Desenvolvimento de Software Ltda`
- **Nome Fantasia**: `Inovar Softwares`
- **CNPJ Formatado**: `28.146.090/0001-42` | **Sem Máscara**: `28146090000142`
- **Data de Início de Atividade**: `2023-01-15`
- **CNAE Principal**: `6201-5/01` - Desenvolvimento de programas de computador sob encomenda
- **Endereço**: Av. Paulista, 1578, Bela Vista, São Paulo/SP - CEP: 01310-200
- **E-mail Institucional**: `contato@inovarsoftware.com.br` | **Telefone**: `(11) 3214-5501`
- **Diagnóstico Fator R**:
  - RBT12: R$ 480.000,00
  - Folha12: R$ 156.000,00
  - Índice Fator R: **32,50%** ($\ge 28\%$)
  - Alíquota Efetiva Inicial: **6,00% (Anexo III)**

```json
{
  "razaoSocial": "Inovar Tecnologia e Desenvolvimento de Software Ltda",
  "nomeFantasia": "Inovar Softwares",
  "cnpj": "28146090000142",
  "dataInicioAtividade": "2023-01-15"
}
```

---

#### Empresa 02 - Clínica Médica e Fisioterapia Ortovita Ltda
- **Razão Social**: `Clínica Médica e Fisioterapia Ortovita Ltda`
- **Nome Fantasia**: `Clínica Ortovita`
- **CNPJ Formatado**: `28.146.107/0001-61` | **Sem Máscara**: `28146107000161`
- **Data de Início de Atividade**: `2022-05-10`
- **CNAE Principal**: `8650-0/04` - Atividades de fisioterapia
- **Endereço**: Rua Visconde de Pirajá, 414, Ipanema, Rio de Janeiro/RJ - CEP: 22410-002
- **E-mail Institucional**: `financeiro@clinicaortovita.com.br` | **Telefone**: `(21) 2521-4402`
- **Diagnóstico Fator R**:
  - RBT12: R$ 720.000,00
  - Folha12: R$ 225.000,00
  - Índice Fator R: **31,25%** ($\ge 28\%$)
  - Alíquota Efetiva: **11,20% (Anexo III)**

```json
{
  "razaoSocial": "Clínica Médica e Fisioterapia Ortovita Ltda",
  "nomeFantasia": "Clínica Ortovita",
  "cnpj": "28146107000161",
  "dataInicioAtividade": "2022-05-10"
}
```

---

#### Empresa 03 - Nexus Treinamentos e Consultoria Empresarial Ltda
- **Razão Social**: `Nexus Treinamentos e Consultoria Empresarial Ltda`
- **Nome Fantasia**: `Nexus Educação Corporativa`
- **CNPJ Formatado**: `28.146.124/0001-07` | **Sem Máscara**: `28146124000107`
- **Data de Início de Atividade**: `2023-08-20`
- **CNAE Principal**: `7020-4/00` - Atividades de consultoria em gestão empresarial
- **Endereço**: Av. Afonso Pena, 2600, Funcionários, Belo Horizonte/MG - CEP: 30130-007
- **E-mail Institucional**: `adm@nexusedu.com.br` | **Telefone**: `(31) 3280-3303`
- **Diagnóstico Fator R**:
  - RBT12: R$ 600.000,00
  - Folha12: R$ 120.000,00
  - Índice Fator R: **20,00%** ($< 28\%$)
  - Enquadramento: **Anexo V (18,00%)**
  - *Oportunidade*: Aumentar a folha/pró-labore em R$ 48.000,00/ano para atingir 28% e migrar para o Anexo III, economizando impostos no Simples Nacional.

```json
{
  "razaoSocial": "Nexus Treinamentos e Consultoria Empresarial Ltda",
  "nomeFantasia": "Nexus Educação Corporativa",
  "cnpj": "28146124000107",
  "dataInicioAtividade": "2023-08-20"
}
```

---

#### Empresa 04 - Studio ArqUrban Arquitetura e Engenharia Ltda
- **Razão Social**: `Studio ArqUrban Arquitetura e Engenharia Ltda`
- **Nome Fantasia**: `Studio ArqUrban`
- **CNPJ Formatado**: `28.146.141/0001-36` | **Sem Máscara**: `28146141000136`
- **Data de Início de Atividade**: `2021-11-01`
- **CNAE Principal**: `7111-1/00` - Serviços de arquitetura
- **Endereço**: Rua Comendador Araújo, 499, Batel, Curitiba/PR - CEP: 80420-000
- **E-mail Institucional**: `contato@arqurban.com.br` | **Telefone**: `(41) 3042-4404`
- **Diagnóstico Fator R**:
  - RBT12: R$ 360.000,00
  - Folha12: R$ 108.000,00
  - Índice Fator R: **30,00%** ($\ge 28\%$)
  - Alíquota Efetiva: **11,20% (Anexo III)**

```json
{
  "razaoSocial": "Studio ArqUrban Arquitetura e Engenharia Ltda",
  "nomeFantasia": "Studio ArqUrban",
  "cnpj": "28146141000136",
  "dataInicioAtividade": "2021-11-01"
}
```

---

#### Empresa 05 - Vertex Agência de Marketing Digital & Estratégia Ltda
- **Razão Social**: `Vertex Agência de Marketing Digital & Estratégia Ltda`
- **Nome Fantasia**: `Vertex Digital Agency`
- **CNPJ Formatado**: `28.146.158/0001-93` | **Sem Máscara**: `28146158000193`
- **Data de Início de Atividade**: `2024-02-15`
- **CNAE Principal**: `7319-0/04` - Consultoria em publicidade e propaganda
- **Endereço**: Av. Carlos Gomes, 1111, Auxiliadora, Porto Alegre/RS - CEP: 90480-004
- **E-mail Institucional**: `contato@vertexdigital.com.br` | **Telefone**: `(51) 3328-5505`
- **Diagnóstico Fator R**:
  - RBT12: R$ 300.000,00
  - Folha12: R$ 66.000,00
  - Índice Fator R: **22,00%** ($< 28\%$)
  - Enquadramento: **Anexo V (18,00%)**

```json
{
  "razaoSocial": "Vertex Agência de Marketing Digital & Estratégia Ltda",
  "nomeFantasia": "Vertex Digital Agency",
  "cnpj": "28146158000193",
  "dataInicioAtividade": "2024-02-15"
}
```

---

#### Empresa 06 - Dental Smile Care Odontologia Especializada Ltda
- **Razão Social**: `Dental Smile Care Odontologia Especializada Ltda`
- **Nome Fantasia**: `Dental Smile Care`
- **CNPJ Formatado**: `28.146.175/0001-20` | **Sem Máscara**: `28146175000120`
- **Data de Início de Atividade**: `2022-03-01`
- **CNAE Principal**: `8630-5/04` - Atividade odontológica especializada com procedimentos cirúrgicos
- **Endereço**: Rua Bocaiúva, 2468, Centro, Florianópolis/SC - CEP: 88015-530
- **E-mail Institucional**: `clinica@dentalsmilecare.com.br` | **Telefone**: `(48) 3224-6606`
- **Diagnóstico Fator R**:
  - RBT12: R$ 540.000,00
  - Folha12: R$ 180.000,00
  - Índice Fator R: **33,33%** ($\ge 28\%$)
  - Alíquota Efetiva: **13,50% (Anexo III)**

```json
{
  "razaoSocial": "Dental Smile Care Odontologia Especializada Ltda",
  "nomeFantasia": "Dental Smile Care",
  "cnpj": "28146175000120",
  "dataInicioAtividade": "2022-03-01"
}
```

---

#### Empresa 07 - Soluções IoT e Automação Industrial Ltda
- **Razão Social**: `Soluções IoT e Automação Industrial Ltda`
- **Nome Fantasia**: `IoT Core Automação`
- **CNPJ Formatado**: `28.146.192/0001-68` | **Sem Máscara**: `28146192000168`
- **Data de Início de Atividade**: `2023-04-12`
- **CNAE Principal**: `7112-0/00` - Serviços de engenharia e automação
- **Endereço**: Av. Tancredo Neves, 1632, Caminho das Árvores, Salvador/BA - CEP: 41820-020
- **E-mail Institucional**: `comercial@solucoesiot.com.br` | **Telefone**: `(71) 3450-7707`
- **Diagnóstico Fator R**:
  - RBT12: R$ 960.000,00
  - Folha12: R$ 310.000,00
  - Índice Fator R: **32,29%** ($\ge 28\%$)
  - Alíquota Efetiva: **16,00% (Anexo III)**

```json
{
  "razaoSocial": "Soluções IoT e Automação Industrial Ltda",
  "nomeFantasia": "IoT Core Automação",
  "cnpj": "28146192000168",
  "dataInicioAtividade": "2023-04-12"
}
```

---

#### Empresa 08 - Farias Perícias Judiciais e Avaliações Técnicas Ltda
- **Razão Social**: `Farias Perícias Judiciais e Avaliações Técnicas Ltda`
- **Nome Fantasia**: `Farias Perícias & Engenharia`
- **CNPJ Formatado**: `28.146.209/0001-87` | **Sem Máscara**: `28146209000187`
- **Data de Início de Atividade**: `2021-07-25`
- **CNAE Principal**: `7490-1/03` - Serviços de perícia técnica relacionados à segurança e avaliação
- **Endereço**: SRTVS Quadra 701, Bloco A, Asa Sul, Brasília/DF - CEP: 70340-907
- **E-mail Institucional**: `laudos@periciasengenharia.com.br` | **Telefone**: `(61) 3322-8808`
- **Diagnóstico Fator R**:
  - RBT12: R$ 420.000,00
  - Folha12: R$ 92.400,00
  - Índice Fator R: **22,00%** ($< 28\%$)
  - Enquadramento: **Anexo V (18,00%)**

```json
{
  "razaoSocial": "Farias Perícias Judiciais e Avaliações Técnicas Ltda",
  "nomeFantasia": "Farias Perícias & Engenharia",
  "cnpj": "28146209000187",
  "dataInicioAtividade": "2021-07-25"
}
```

---

#### Empresa 09 - Data Prime Ciência de Dados e IA Ltda
- **Razão Social**: `Data Prime Ciência de Dados e IA Ltda`
- **Nome Fantasia**: `Data Prime Analytics`
- **CNPJ Formatado**: `28.146.226/0001-14` | **Sem Máscara**: `28146226000114`
- **Data de Início de Atividade**: `2024-01-10`
- **CNAE Principal**: `6202-3/00` - Desenvolvimento e licenciamento de inteligência artificial e análise de dados
- **Endereço**: Av. Deputado Jamel Cecílio, 3455, Jardim Goiás, Goiânia/GO - CEP: 74810-100
- **E-mail Institucional**: `operacoes@dataprimeanalytics.com.br` | **Telefone**: `(62) 3945-9909`
- **Diagnóstico Fator R**:
  - RBT12: R$ 1.200.000,00
  - Folha12: R$ 420.000,00
  - Índice Fator R: **35,00%** ($\ge 28\%$)
  - Alíquota Efetiva: **16,00% (Anexo III)**

```json
{
  "razaoSocial": "Data Prime Ciência de Dados e IA Ltda",
  "nomeFantasia": "Data Prime Analytics",
  "cnpj": "28146226000114",
  "dataInicioAtividade": "2024-01-10"
}
```

---

#### Empresa 10 - Clínica Veterinária e Diagnóstico AnimaVita Ltda
- **Razão Social**: `Clínica Veterinária e Diagnóstico AnimaVita Ltda`
- **Nome Fantasia**: `Centro Veterinário AnimaVita`
- **CNPJ Formatado**: `28.146.243/0001-51` | **Sem Máscara**: `28146243000151`
- **Data de Início de Atividade**: `2022-09-01`
- **CNAE Principal**: `7500-1/00` - Atividades veterinárias e diagnósticos laboratoriais
- **Endereço**: Av. Conselheiro Aguiar, 2333, Boa Viagem, Recife/PE - CEP: 51020-020
- **E-mail Institucional**: `atendimento@clinicaanimavita.com.br` | **Telefone**: `(81) 3465-1010`
- **Diagnóstico Fator R**:
  - RBT12: R$ 380.000,00
  - Folha12: R$ 114.000,00
  - Índice Fator R: **30,00%** ($\ge 28\%$)
  - Alíquota Efetiva: **11,20% (Anexo III)**

```json
{
  "razaoSocial": "Clínica Veterinária e Diagnóstico AnimaVita Ltda",
  "nomeFantasia": "Centro Veterinário AnimaVita",
  "cnpj": "28146243000151",
  "dataInicioAtividade": "2022-09-01"
}
```

---

## 7. Exemplos Prontos de Requisições de Teste (API REST / cURL)

### 7.1. Login com CPF
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "21543810063",
    "senha": "Teste@123"
  }'
```

### 7.2. Login com E-mail
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "bruno.castro@inovarsoftware.com.br",
    "senha": "Teste@123"
  }'
```

### 7.3. Cadastro de Contador (API)
```bash
curl -X POST http://localhost:8080/api/auth/register/contador \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Carlos Eduardo Moreira",
    "email": "carlos.moreira@moreiracontabil.com.br",
    "cpf": "21543810063",
    "senha": "Teste@123",
    "nomeOrganizacao": "Moreira & Associados Assessoria Contábil Ltda",
    "cnpj": "28145920000117",
    "crc": "CRC-SP 128941/O"
  }'
```

### 7.4. Cadastro de Empresário (API)
```bash
curl -X POST http://localhost:8080/api/auth/register/empresario \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Bruno Henrique Castro",
    "email": "bruno.castro@inovarsoftware.com.br",
    "cpf": "21543923003",
    "senha": "Teste@123",
    "razaoSocial": "Inovar Tecnologia e Desenvolvimento de Software Ltda",
    "nomeFantasia": "Inovar Softwares",
    "cnpj": "28146090000142"
  }'
```

### 7.5. Cadastro de Nova Empresa (API)
```bash
curl -X POST http://localhost:8080/api/empresas \
  -H "Content-Type: application/json" \
  -d '{
    "razaoSocial": "Inovar Tecnologia e Desenvolvimento de Software Ltda",
    "nomeFantasia": "Inovar Softwares",
    "cnpj": "28146090000142",
    "dataInicioAtividade": "2023-01-15"
  }'
```

### 7.6. Simulação Tributária do Fator R (API)
```bash
# Simular para a Empresa ID 1 na competência 05/2026
curl -X GET "http://localhost:8080/api/empresas/1/simulacao-tributaria?ano=2026&mes=5" \
  -H "Content-Type: application/json"
```

---
*Arquivo gerado especialmente para os testes do Sistema de Controle Fator R.*
