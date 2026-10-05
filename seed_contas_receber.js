const fs = require('fs');
const { Client } = require('pg');
const crypto = require('crypto');
const path = require('path');

const client = new Client({ user: 'postgres', password: 'admin', host: 'localhost', port: 5432, database: 'fatorr' });

async function run() {
  await client.connect();
  
  const res = await client.query("SELECT id, uuid_id, razao_social, cnpj FROM empresas WHERE cnpj LIKE '%22222222222222%' OR cnpj LIKE '%55555555555555%' OR cnpj LIKE '%98XYZ765000102%' OR cnpj LIKE '%22.222.222/2222-22%' OR cnpj LIKE '%55.555.555/5555-55%'");
  
  const empresas = res.rows;
  console.log('Empresas encontradas:', empresas);

  const outDir = path.join(__dirname, 'arquivos para testes');
  
  let i = 1;
  for (const emp of empresas) {
    const empresaIdStr = emp.uuid_id ? emp.uuid_id : String(emp.id);
    
    const clienteId = crypto.randomUUID(); 
    try {
        await client.query("INSERT INTO clientes (id, empresa_id, nome, cpf_cnpj, status) VALUES ($1, $2, $3, $4, 'ATIVO') ON CONFLICT DO NOTHING", [clienteId, emp.id, 'Cliente Teste ' + emp.razao_social, '00000000000']);
    } catch(e) {
        console.log('Nao foi possivel inserir cliente de teste (ignorando)', e.message);
    }
    
    const contas = [];
    
    for (let j = 0; j < 5; j++) {
      const id = crypto.randomUUID();
      const valor = (Math.random() * 5000 + 1000).toFixed(2);
      const numeroDoc = "NF " + Math.floor(Math.random() * 9999);
      const hoje = new Date().toISOString().split('T')[0];
      
      try {
        await client.query("INSERT INTO contas_a_receber (id, empresa_id, cliente_id, numero_documento, valor_total, data_emissao, data_vencimento, status) VALUES ($1, $2, $3, $4, $5, $6, $7, 'PENDENTE')", [id, empresaIdStr, clienteId, numeroDoc, valor, hoje, hoje]);
        contas.push({ desc: numeroDoc, valor, date: hoje });
      } catch (e) {
         console.error('Erro ao inserir conta:', e.message);
      }
    }
    
    console.log("Geradas " + contas.length + " contas para a empresa " + emp.cnpj);
    
    const isOfx = i % 2 !== 0; 
    
    if (isOfx) {
      const filePath = path.join(outDir, "extrato_" + emp.cnpj.replace(/\D/g, '') + ".ofx");
      let ofxContent = "OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nSECURITY:NONE\nENCODING:USASCII\nCHARSET:1252\nCOMPRESSION:NONE\nOLDFILEUID:NONE\nNEWFILEUID:NONE\n\n";
      ofxContent += "<OFX>\n<BANKMSGSRSV1>\n<STMTTRNRS>\n<STMTRS>\n";
      ofxContent += "<BANKACCTFROM>\n<BANKID>341\n<ACCTID>12345-6\n</BANKACCTFROM>\n";
      ofxContent += "<BANKTRANLIST>\n";
      
      for (const c of contas) {
        const fitid = crypto.randomUUID().substring(0, 10);
        const dt = c.date.replace(/-/g, '') + "120000";
        ofxContent += "<STMTTRN>\n<TRNTYPE>CREDIT\n<DTPOSTED>" + dt + "\n<TRNAMT>" + c.valor + "\n<FITID>" + fitid + "\n<MEMO>" + c.desc + "\n</STMTTRN>\n";
      }
      
      ofxContent += "</BANKTRANLIST>\n</STMTRS>\n</STMTTRNRS>\n</BANKMSGSRSV1>\n</OFX>";
      fs.writeFileSync(filePath, ofxContent);
    } else {
      const filePath = path.join(outDir, "extrato_" + emp.cnpj.replace(/\D/g, '') + ".csv");
      let csvContent = "data,descricao,valor,tipo\n";
      for (const c of contas) {
        csvContent += c.date + "," + c.desc + "," + c.valor + ",CREDITO\n";
      }
      fs.writeFileSync(filePath, csvContent);
    }
    
    i++;
  }
  
  await client.end();
  console.log('Script finalizado com sucesso!');
}

run().catch(console.error);
