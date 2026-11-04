const fs = require('fs');
const path = require('path');

// Diretório base
const BASE_DIR = path.join(__dirname);
const FATURAMENTO_DIR = path.join(BASE_DIR, 'faturamento');
const FOLHA_DIR = path.join(BASE_DIR, 'folha de pagamento');
const EXTRATO_DIR = path.join(BASE_DIR, 'extrato bancario');

// Clientes Tomadores de Teste com CNPJs válidos
const TOMADORES = [
  { cnpj: '00000000000191', nome: 'Banco do Brasil SA' },
  { cnpj: '33000167000101', nome: 'Petrobras Petroleo Brasileiro SA' },
  { cnpj: '47960950000121', nome: 'Magazine Luiza SA' },
  { cnpj: '56994962000146', nome: 'Ambev SA' },
  { cnpj: '33592510000154', nome: 'Vale SA' },
  { cnpj: '06057223000171', nome: 'Sendas Distribuidora SA' }
];

// 12 Empresas (10 de dadosparacraircontas.md + 2 seeds Alpha e Beta)
const EMPRESAS = [
  {
    pasta: '01 - Inovar Softwares',
    razaoSocial: 'Inovar Tecnologia e Desenvolvimento de Software Ltda',
    nomeFantasia: 'Inovar Softwares',
    cnpj: '28146090000142',
    rbt12: 480000.00,
    folha12: 156000.00,
    socio: { nome: 'Bruno Henrique Castro', cpf: '21543923003', proLabore: 8000.00 },
    colab: { nome: 'Lucas Gabriel Fonseca', cpf: '21544036035', salario: 4200.00 },
    encargos: { inss: 500.00, fgts: 300.00 }
  },
  {
    pasta: '02 - Clinica Ortovita',
    razaoSocial: 'Clínica Médica e Fisioterapia Ortovita Ltda',
    nomeFantasia: 'Clínica Ortovita',
    cnpj: '28146107000161',
    rbt12: 720000.00,
    folha12: 225000.00,
    socio: { nome: 'Patrícia Mendes Lima', cpf: '21543934382', proLabore: 14000.00 },
    colab: { nome: 'Juliana Martins Ribeiro', cpf: '21544047312', salario: 3400.00 },
    encargos: { inss: 900.00, fgts: 450.00 }
  },
  {
    pasta: '03 - Nexus Educacao',
    razaoSocial: 'Nexus Treinamentos e Consultoria Empresarial Ltda',
    nomeFantasia: 'Nexus Educação Corporativa',
    cnpj: '28146124000107',
    rbt12: 600000.00,
    folha12: 120000.00,
    socio: { nome: 'Rodrigo Alves Nogueira', cpf: '21543945660', proLabore: 4200.00 },
    colab: { nome: 'Thiago Siqueira Prado', cpf: '21544058608', salario: 5100.00 },
    encargos: { inss: 400.00, fgts: 300.00 }
  },
  {
    pasta: '04 - Studio ArqUrban',
    razaoSocial: 'Studio ArqUrban Arquitetura e Engenharia Ltda',
    nomeFantasia: 'Studio ArqUrban',
    cnpj: '28146141000136',
    rbt12: 360000.00,
    folha12: 108000.00,
    socio: { nome: 'Juliana Barbosa Paiva', cpf: '21543956947', proLabore: 2100.00 },
    colab: { nome: 'Larissa Nogueira Sales', cpf: '21544069987', salario: 6300.00 },
    encargos: { inss: 350.00, fgts: 250.00 }
  },
  {
    pasta: '05 - Vertex Digital',
    razaoSocial: 'Vertex Agência de Marketing Digital & Estratégia Ltda',
    nomeFantasia: 'Vertex Digital Agency',
    cnpj: '28146158000193',
    rbt12: 300000.00,
    folha12: 66000.00,
    socio: { nome: 'Gustavo Henrique Toledo', cpf: '21543968287', proLabore: 0.00 },
    colab: { nome: 'Gabriel Monteiro Bastos', cpf: '21544081251', salario: 4800.00 },
    encargos: { inss: 400.00, fgts: 300.00 }
  },
  {
    pasta: '06 - Dental Smile',
    razaoSocial: 'Dental Smile Care Odontologia Especializada Ltda',
    nomeFantasia: 'Dental Smile Care',
    cnpj: '28146175000120',
    rbt12: 540000.00,
    folha12: 180000.00,
    socio: { nome: 'Renata Carvalho Pires', cpf: '21543979564', proLabore: 11000.00 },
    colab: { nome: 'Priscila Dorneles Ramos', cpf: '21544092539', salario: 2900.00 },
    encargos: { inss: 700.00, fgts: 400.00 }
  },
  {
    pasta: '07 - IoT Core',
    razaoSocial: 'Soluções IoT e Automação Industrial Ltda',
    nomeFantasia: 'IoT Core Automação',
    cnpj: '28146192000168',
    rbt12: 960000.00,
    folha12: 310000.00,
    socio: { nome: 'Felipe Antunes Barreto', cpf: '21543990886', proLabore: 16500.00 },
    colab: { nome: 'Matheus Henrique Vieira', cpf: '21544103832', salario: 7500.00 },
    encargos: { inss: 1100.00, fgts: 733.33 }
  },
  {
    pasta: '08 - Farias Pericias',
    razaoSocial: 'Farias Perícias Judiciais e Avaliações Técnicas Ltda',
    nomeFantasia: 'Farias Perícias & Engenharia',
    cnpj: '28146209000187',
    rbt12: 420000.00,
    folha12: 92400.00,
    socio: { nome: 'Aline Vasconcelos Farias', cpf: '21544002130', proLabore: 3500.00 },
    colab: { nome: 'Camila Silveira Fontes', cpf: '21544115172', salario: 3800.00 },
    encargos: { inss: 250.00, fgts: 150.00 }
  },
  {
    pasta: '09 - Data Prime',
    razaoSocial: 'Data Prime Ciência de Dados e IA Ltda',
    nomeFantasia: 'Data Prime Analytics',
    cnpj: '28146226000114',
    rbt12: 1200000.00,
    folha12: 420000.00,
    socio: { nome: 'Thiago Mendonça Leal', cpf: '21544013418', proLabore: 26500.00 },
    colab: { nome: 'Daniel Correia Guedes', cpf: '21544126450', salario: 5600.00 },
    encargos: { inss: 1800.00, fgts: 1100.00 }
  },
  {
    pasta: '10 - Centro AnimaVita',
    razaoSocial: 'Clínica Veterinária e Diagnóstico AnimaVita Ltda',
    nomeFantasia: 'Centro Veterinário AnimaVita',
    cnpj: '28146243000151',
    rbt12: 380000.00,
    folha12: 114000.00,
    socio: { nome: 'Vanessa Couto Marinho', cpf: '21544024703', proLabore: 5700.00 },
    colab: { nome: 'Fernanda Tavares Paes', cpf: '21544137737', salario: 3200.00 },
    encargos: { inss: 350.00, fgts: 250.00 }
  },
  {
    pasta: '11 - Empresa Alpha Servicos',
    razaoSocial: 'Empresa Alpha Serviços',
    nomeFantasia: 'Alpha Serviços Contábeis',
    cnpj: '12ABC345000101',
    rbt12: 480000.00,
    folha12: 156000.00,
    socio: { nome: 'Empresário Silva', cpf: '22233344455', proLabore: 8000.00 },
    colab: { nome: 'Colaborador Convidado', cpf: '99988877766', salario: 4200.00 },
    encargos: { inss: 500.00, fgts: 300.00 }
  },
  {
    pasta: '12 - Empresa Beta Tecnologia',
    razaoSocial: 'Empresa Beta Tecnologia',
    nomeFantasia: 'Beta Tech Solutions',
    cnpj: '98XYZ765000102',
    rbt12: 600000.00,
    folha12: 120000.00,
    socio: { nome: 'Empresário Silva', cpf: '22233344455', proLabore: 4200.00 },
    colab: { nome: 'Colaborador Convidado', cpf: '99988877766', salario: 5100.00 },
    encargos: { inss: 400.00, fgts: 300.00 }
  }
];

const ANOS = [2025, 2026];
const MESES = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];

function formatMoeda(num) {
  return Number(num).toFixed(2);
}

function formatCnpjMask(cnpj) {
  if (cnpj.length === 14) {
    return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
  }
  return cnpj;
}

function gerarChaveNfe(cnpj, ano, mes, numero) {
  const cnpjLimpo = cnpj.replace(/\D/g, '').padEnd(14, '0').substring(0, 14);
  const aa = String(ano).substring(2);
  const mm = mes;
  const mod = '55';
  const serie = '001';
  const numPad = String(numero).padStart(9, '0');
  const tpEmis = '1';
  const cNF = String(10000000 + Number(numero)).substring(1);
  const base = `35${aa}${mm}${cnpjLimpo}${mod}${serie}${numPad}${tpEmis}${cNF}`;
  return base.padEnd(44, '9').substring(0, 44);
}

function gerarXmlNfe(empresa, tomador, ano, mes, dia, numero, valor, vencimento) {
  const chave = gerarChaveNfe(empresa.cnpj, ano, mes, numero);
  const dataEmi = `${ano}-${mes}-${dia}T10:00:00-03:00`;
  const dataVenc = vencimento || `${ano}-${mes}-${dia}`;

  return `<?xml version="1.0" encoding="UTF-8"?>
<nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
  <NFe>
    <infNFe Id="NFe${chave}">
      <ide>
        <nNF>${numero}</nNF>
        <dhEmi>${dataEmi}</dhEmi>
        <dVenc>${dataVenc}</dVenc>
      </ide>
      <emit>
        <CNPJ>${empresa.cnpj}</CNPJ>
        <xNome>${empresa.razaoSocial}</xNome>
        <xFant>${empresa.nomeFantasia}</xFant>
      </emit>
      <dest>
        <CNPJ>${tomador.cnpj}</CNPJ>
        <xNome>${tomador.nome}</xNome>
      </dest>
      <total>
        <ICMSTot>
          <vNF>${formatMoeda(valor)}</vNF>
        </ICMSTot>
      </total>
    </infNFe>
  </NFe>
</nfeProc>`;
}

function gerarXmlNfse(empresa, tomador, ano, mes, dia, numero, valor, vencimento) {
  const chave = gerarChaveNfe(empresa.cnpj, ano, mes, numero);
  const dataEmi = `${ano}-${mes}-${dia}T14:30:00-03:00`;
  const dataVenc = vencimento || `${ano}-${mes}-${dia}`;

  return `<?xml version="1.0" encoding="UTF-8"?>
<NFSe xmlns="http://www.sped.fazenda.gov.br/nfse">
  <infNFSe Id="NFSe${chave}">
    <nNFSe>${numero}</nNFSe>
    <emit>
      <CNPJ>${empresa.cnpj}</CNPJ>
      <xNome>${empresa.razaoSocial}</xNome>
    </emit>
    <DPS>
      <infDPS>
        <ide>
          <dhEmi>${dataEmi}</dhEmi>
          <dataVencimento>${dataVenc}</dataVencimento>
        </ide>
        <toma>
          <CNPJ>${tomador.cnpj}</CNPJ>
          <xNome>${tomador.nome}</xNome>
        </toma>
        <valores>
          <vServPrest>
            <vServ>${formatMoeda(valor)}</vServ>
          </vServPrest>
        </valores>
      </infDPS>
    </DPS>
  </infNFSe>
</NFSe>`;
}

function gerarXmlNfeParcelada(empresa, tomador, ano, mes, dia, numero, valorTotal, parcelas) {
  const chave = gerarChaveNfe(empresa.cnpj, ano, mes, numero);
  const dataEmi = `${ano}-${mes}-${dia}T09:15:00-03:00`;

  let dupXml = '';
  for (const p of parcelas) {
    dupXml += `        <dup>
          <nDup>${p.numero}</nDup>
          <dVenc>${p.vencimento}</dVenc>
          <vDup>${formatMoeda(p.valor)}</vDup>
        </dup>\n`;
  }

  return `<?xml version="1.0" encoding="UTF-8"?>
<nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
  <NFe>
    <infNFe Id="NFe${chave}">
      <ide>
        <nNF>${numero}</nNF>
        <dhEmi>${dataEmi}</dhEmi>
      </ide>
      <emit>
        <CNPJ>${empresa.cnpj}</CNPJ>
        <xNome>${empresa.razaoSocial}</xNome>
        <xFant>${empresa.nomeFantasia}</xFant>
      </emit>
      <dest>
        <CNPJ>${tomador.cnpj}</CNPJ>
        <xNome>${tomador.nome}</xNome>
      </dest>
      <total>
        <ICMSTot>
          <vNF>${formatMoeda(valorTotal)}</vNF>
        </ICMSTot>
      </total>
      <cobr>
${dupXml}      </cobr>
    </infNFe>
  </NFe>
</nfeProc>`;
}

function gerarXmlEsocial(empresa, ano, mes) {
  const competencia = `${ano}-${mes}`;
  const recId = `ID1${empresa.cnpj.replace(/\D/g, '').padEnd(14, '0')}${ano}${mes}010001`;

  let trabXml = '';
  // Sócio (se houver pró-labore)
  if (empresa.socio && empresa.socio.proLabore > 0) {
    trabXml += `    <ideTrabalhador>
      <cpfTrab>${empresa.socio.cpf}</cpfTrab>
      <dmDev>
        <codCateg>721</codCateg>
        <infoPerApur>
          <itensRemun>
            <codRubr>3501</codRubr>
            <natRubr>3501</natRubr>
            <dscRubr>Retirada de Pro-Labore</dscRubr>
            <vrRubr>${formatMoeda(empresa.socio.proLabore)}</vrRubr>
          </itensRemun>
        </infoPerApur>
      </dmDev>
    </ideTrabalhador>\n`;
  }

  // Colaborador CLT
  if (empresa.colab && empresa.colab.salario > 0) {
    trabXml += `    <ideTrabalhador>
      <cpfTrab>${empresa.colab.cpf}</cpfTrab>
      <dmDev>
        <codCateg>101</codCateg>
        <infoPerApur>
          <itensRemun>
            <codRubr>1000</codRubr>
            <natRubr>1000</natRubr>
            <dscRubr>Salario Base CLT</dscRubr>
            <vrRubr>${formatMoeda(empresa.colab.salario)}</vrRubr>
          </itensRemun>
        </infoPerApur>
      </dmDev>
    </ideTrabalhador>\n`;
  }

  return `<?xml version="1.0" encoding="UTF-8"?>
<eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtRemun/v_S_01_02_00">
  <evtRemun Id="${recId}">
    <ideEvento>
      <perApur>${competencia}</perApur>
    </ideEvento>
    <ideEmpregador>
      <tpInsc>1</tpInsc>
      <nrInsc>${empresa.cnpj}</nrInsc>
    </ideEmpregador>
${trabXml}    <infoTotal>
      <vrCp>${formatMoeda(empresa.encargos.inss)}</vrCp>
      <vrFgts>${formatMoeda(empresa.encargos.fgts)}</vrFgts>
    </infoTotal>
  </evtRemun>
</eSocial>`;
}

function gerarCsvFolhaMensal(empresa, ano, mes) {
  const competencia = `${ano}-${mes}`;
  const decimoTerceiro = (mes === '12') ? empresa.colab.salario : 0.00;
  return `competencia;valorSalarios;valorProLabore;valorDecimoTerceiro;valorInssPatronal;valorFgts\n${competencia};${formatMoeda(empresa.colab.salario)};${formatMoeda(empresa.socio.proLabore)};${formatMoeda(decimoTerceiro)};${formatMoeda(empresa.encargos.inss)};${formatMoeda(empresa.encargos.fgts)}\n`;
}

function gerarOfx(empresa, transacoes) {
  let trnXml = '';
  for (const t of transacoes) {
    const dt = t.data.replace(/-/g, '') + '120000';
    trnXml += `<STMTTRN>
<TRNTYPE>${t.tipo}</TRNTYPE>
<DTPOSTED>${dt}</DTPOSTED>
<TRNAMT>${t.tipo === 'DEBIT' ? '-' + formatMoeda(t.valor) : formatMoeda(t.valor)}</TRNAMT>
<FITID>${t.fitid}</FITID>
<MEMO>${t.descricao}</MEMO>
</STMTTRN>\n`;
  }

  return `OFXHEADER:100
DATA:OFXSGML
VERSION:102
SECURITY:NONE
ENCODING:USASCII
CHARSET:1252
COMPRESSION:NONE
OLDFILEUID:NONE
NEWFILEUID:NONE

<OFX>
<BANKMSGSRSV1>
<STMTTRNRS>
<STMTRS>
<BANKACCTFROM>
<BANKID>341</BANKID>
<ACCTID>12345-6</ACCTID>
</BANKACCTFROM>
<BANKTRANLIST>
${trnXml}</BANKTRANLIST>
</STMTRS>
</STMTTRNRS>
</BANKMSGSRSV1>
</OFX>`;
}

function gerarCsvExtrato(transacoes) {
  let csv = 'data,descricao,valor,tipo,fitid\n';
  for (const t of transacoes) {
    csv += `${t.data},${t.descricao},${formatMoeda(t.valor)},${t.tipo === 'DEBIT' ? 'DEBITO' : 'CREDITO'},${t.fitid}\n`;
  }
  return csv;
}

// EXECUÇÃO PRINCIPAL
function run() {
  console.log('Iniciando geração de arquivos de teste...');
  
  let totalXmls = 0;
  let totalFolhas = 0;
  let totalExtratos = 0;

  for (const emp of EMPRESAS) {
    console.log(`\nProcessando Empresa: ${emp.pasta}`);

    const faturamentoMesTotal = emp.rbt12 / 12;
    // 40% NF-e, 35% NFS-e, 25% NF-e Parcelada (12.5% + 12.5%)
    const valNfe1 = Number((faturamentoMesTotal * 0.40).toFixed(2));
    const valNfse2 = Number((faturamentoMesTotal * 0.35).toFixed(2));
    const valNfe3Total = Number((faturamentoMesTotal - valNfe1 - valNfse2).toFixed(2));
    const valNfe3Parc1 = Number((valNfe3Total / 2).toFixed(2));
    const valNfe3Parc2 = Number((valNfe3Total - valNfe3Parc1).toFixed(2));

    for (const ano of ANOS) {
      // Cria pastas raiz do ano para cada tipo
      const dirFatAno = path.join(FATURAMENTO_DIR, emp.pasta, String(ano));
      const dirFolhaAno = path.join(FOLHA_DIR, emp.pasta, String(ano));
      const dirExtAno = path.join(EXTRATO_DIR, emp.pasta, String(ano));

      fs.mkdirSync(dirFatAno, { recursive: true });
      fs.mkdirSync(dirFolhaAno, { recursive: true });
      fs.mkdirSync(dirExtAno, { recursive: true });

      let csvFolhaAnualLinhas = ['competencia;valorSalarios;valorProLabore;valorDecimoTerceiro;valorInssPatronal;valorFgts'];

      for (let mIdx = 0; mIdx < MESES.length; mIdx++) {
        const mes = MESES[mIdx];
        const numBase = (ano % 100) * 1000 + (mIdx + 1) * 30;

        // Subpastas dos meses
        const dirFatMes = path.join(dirFatAno, mes);
        const dirFolhaMes = path.join(dirFolhaAno, mes);
        const dirExtMes = path.join(dirExtAno, mes);

        fs.mkdirSync(dirFatMes, { recursive: true });
        fs.mkdirSync(dirFolhaMes, { recursive: true });
        fs.mkdirSync(dirExtMes, { recursive: true });

        // 1. FATURAMENTO (3 XMLs por mês)
        const tomador1 = TOMADORES[mIdx % TOMADORES.length];
        const tomador2 = TOMADORES[(mIdx + 1) % TOMADORES.length];
        const tomador3 = TOMADORES[(mIdx + 2) % TOMADORES.length];

        const numNfe1 = String(numBase + 1);
        const numNfse2 = String(numBase + 2);
        const numNfe3 = String(numBase + 3);

        const dtVenc1 = `${ano}-${mes}-10`;
        const dtVenc2 = `${ano}-${mes}-20`;
        const dtVenc3P1 = `${ano}-${mes}-25`;
        
        // Vencimento da parcela 2 (mês seguinte)
        let anoP2 = ano;
        let mesP2Num = mIdx + 2;
        if (mesP2Num > 12) {
          mesP2Num = 1;
          anoP2 = ano + 1;
        }
        const mesP2Str = String(mesP2Num).padStart(2, '0');
        const dtVenc3P2 = `${anoP2}-${mesP2Str}-25`;

        // Arquivo 1: NF-e à vista
        const xml1 = gerarXmlNfe(emp, tomador1, ano, mes, '05', numNfe1, valNfe1, dtVenc1);
        fs.writeFileSync(path.join(dirFatMes, `nfe_${ano}_${mes}_${numNfe1}.xml`), xml1, 'utf8');

        // Arquivo 2: NFS-e padrão nacional
        const xml2 = gerarXmlNfse(emp, tomador2, ano, mes, '12', numNfse2, valNfse2, dtVenc2);
        fs.writeFileSync(path.join(dirFatMes, `nfse_${ano}_${mes}_${numNfse2}.xml`), xml2, 'utf8');

        // Arquivo 3: NF-e parcelada em 2 duplicatas
        const parcelasNfe3 = [
          { numero: '1', vencimento: dtVenc3P1, valor: valNfe3Parc1 },
          { numero: '2', vencimento: dtVenc3P2, valor: valNfe3Parc2 }
        ];
        const xml3 = gerarXmlNfeParcelada(emp, tomador3, ano, mes, '15', numNfe3, valNfe3Total, parcelasNfe3);
        fs.writeFileSync(path.join(dirFatMes, `nfe_parcelada_${ano}_${mes}_${numNfe3}.xml`), xml3, 'utf8');

        totalXmls += 3;

        // 2. FOLHA DE PAGAMENTO (eSocial XML + CSV Mensal)
        const xmlEsocial = gerarXmlEsocial(emp, ano, mes);
        fs.writeFileSync(path.join(dirFolhaMes, `esocial_s1200_${ano}_${mes}.xml`), xmlEsocial, 'utf8');

        const csvMensal = gerarCsvFolhaMensal(emp, ano, mes);
        fs.writeFileSync(path.join(dirFolhaMes, `folha_${ano}_${mes}.csv`), csvMensal, 'utf8');

        const dec13 = (mes === '12') ? emp.colab.salario : 0.00;
        csvFolhaAnualLinhas.push(`${ano}-${mes};${formatMoeda(emp.colab.salario)};${formatMoeda(emp.socio.proLabore)};${formatMoeda(dec13)};${formatMoeda(emp.encargos.inss)};${formatMoeda(emp.encargos.fgts)}`);

        totalFolhas += 2;

        // 3. EXTRATO BANCÁRIO (OFX e CSV perfeitamente conciliáveis)
        const transacoes = [
          // Recebimento da NF 1
          {
            tipo: 'CREDIT',
            data: dtVenc1,
            valor: valNfe1,
            fitid: `FIT-${ano}${mes}10-${numNfe1}`,
            descricao: `DOC/TED CRÉDITO NF ${numNfe1} - ${tomador1.nome.substring(0, 15)}`
          },
          // Recebimento da NFSe 2
          {
            tipo: 'CREDIT',
            data: dtVenc2,
            valor: valNfse2,
            fitid: `FIT-${ano}${mes}20-${numNfse2}`,
            descricao: `PIX RECEBIDO NFSE ${numNfse2} - ${tomador2.nome.substring(0, 15)}`
          },
          // Recebimento da Parcela 1 da NF 3
          {
            tipo: 'CREDIT',
            data: dtVenc3P1,
            valor: valNfe3Parc1,
            fitid: `FIT-${ano}${mes}25-${numNfe3}-1`,
            descricao: `DOC/TED CRÉDITO NF ${numNfe3}/1 - ${tomador3.nome.substring(0, 15)}`
          },
          // Pagamento de Salários / Pró-labore
          {
            tipo: 'DEBIT',
            data: `${ano}-${mes}-05`,
            valor: Number((emp.colab.salario + emp.socio.proLabore).toFixed(2)),
            fitid: `DEB-${ano}${mes}05-FOLHA`,
            descricao: `TRANSF FOLHA SALARIOS E PRO-LABORE`
          },
          // Pagamento Guia Simples Nacional (DAS)
          {
            tipo: 'DEBIT',
            data: `${ano}-${mes}-20`,
            valor: Number((faturamentoMesTotal * 0.06).toFixed(2)),
            fitid: `DEB-${ano}${mes}20-DAS`,
            descricao: `PGTO GUIA DAS SIMPLES NACIONAL`
          }
        ];

        const ofxContent = gerarOfx(emp, transacoes);
        fs.writeFileSync(path.join(dirExtMes, `extrato_${ano}_${mes}.ofx`), ofxContent, 'utf8');

        const csvExtratoContent = gerarCsvExtrato(transacoes);
        fs.writeFileSync(path.join(dirExtMes, `extrato_${ano}_${mes}.csv`), csvExtratoContent, 'utf8');

        totalExtratos += 2;
      }

      // Salva CSV Consolidado Anual da Folha na raiz do ano
      fs.writeFileSync(path.join(dirFolhaAno, `folha_anual_${ano}.csv`), csvFolhaAnualLinhas.join('\n') + '\n', 'utf8');
      totalFolhas += 1;
    }
  }

  // Remove os 3 arquivos antigos soltos da raiz de "arquivos para testes", se existirem
  const arquivosAntigos = [
    'extrato_22222222222222.ofx',
    'extrato_55555555555555.csv',
    'extrato_98765000102.ofx'
  ];
  for (const arq of arquivosAntigos) {
    const p = path.join(BASE_DIR, arq);
    if (fs.existsSync(p)) {
      fs.unlinkSync(p);
      console.log(`Arquivo antigo removido: ${arq}`);
    }
  }

  console.log('\n=============================================');
  console.log(`SUCESSO! Geração concluída:`);
  console.log(`- XMLs de Faturamento gerados: ${totalXmls}`);
  console.log(`- Arquivos de Folha gerados: ${totalFolhas}`);
  console.log(`- Extratos Bancários gerados: ${totalExtratos}`);
  console.log(`Total de arquivos: ${totalXmls + totalFolhas + totalExtratos}`);
  console.log('=============================================\n');
}

run();
