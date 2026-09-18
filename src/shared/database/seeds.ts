import { 
  OrganizacaoContabilDTO, 
  EmpresaDTO, 
  EmpresaUsuarioDTO 
} from '../types/common.types';
import { 
  RegisterContadorDTO, 
  RegisterEmpresarioDTO, 
  TipoPerfil, 
  StatusUsuario, 
  StatusVinculo 
} from '../types/auth.types';

export const SEED_ORGANIZACAO_CONTABIL: OrganizacaoContabilDTO = {
  id: 'org-modelo-uuid-0001',
  nome: 'Organização Contábil Modelo Ltda',
  cnpj: '12ABC345000199', // Alfanumérico
  crc: 'CRC-SP 123456/O'
};

export const SEED_USUARIO_CONTADOR = {
  id: 'usr-contador-uuid-0001',
  nome: 'Contador Chefe',
  email: 'contador@teste.com',
  cpf: '11122233344',
  senhaHash: '123456',
  tipoPerfil: 'CONTABILISTA' as TipoPerfil,
  organizacaoContabilId: 'org-modelo-uuid-0001',
  status: 'ATIVO' as StatusUsuario
};

export const SEED_EMPRESAS: EmpresaDTO[] = [
  {
    id: 'emp-alpha-uuid-0001',
    razaoSocial: 'Empresa Alpha Serviços',
    nomeFantasia: 'Alpha Serviços Contábeis',
    cnpj: '12ABC345000101', // Alfanumérico
    organizacaoContabilId: 'org-modelo-uuid-0001'
  },
  {
    id: 'emp-beta-uuid-0002',
    razaoSocial: 'Empresa Beta Tecnologia',
    nomeFantasia: 'Beta Tech Solutions',
    cnpj: '98XYZ765000102', // Alfanumérico
    organizacaoContabilId: 'org-modelo-uuid-0001'
  }
];

export const SEED_USUARIO_EMPRESARIO = {
  id: 'usr-empresario-uuid-0002',
  nome: 'Empresário Silva',
  email: 'empresario@teste.com',
  cpf: '22233344455',
  senhaHash: '123456',
  tipoPerfil: 'EMPRESARIO' as TipoPerfil,
  status: 'ATIVO' as StatusUsuario
};

export const SEED_VINCULOS_EMPRESARIO: EmpresaUsuarioDTO[] = [
  {
    id: 'vinc-emp-alpha-0001',
    empresaId: 'emp-alpha-uuid-0001',
    usuarioId: 'usr-empresario-uuid-0002',
    funcao: 'Sócio Administrador',
    statusVinculo: 'APROVADO' as StatusVinculo,
    aprovadoPor: 'usr-contador-uuid-0001'
  },
  {
    id: 'vinc-emp-beta-0002',
    empresaId: 'emp-beta-uuid-0002',
    usuarioId: 'usr-empresario-uuid-0002',
    funcao: 'Sócio Diretor',
    statusVinculo: 'APROVADO' as StatusVinculo,
    aprovadoPor: 'usr-contador-uuid-0001'
  }
];

export const SEED_USUARIO_COLABORADOR_PRECADASTRO = {
  id: 'usr-colaborador-uuid-0003',
  nome: 'Colaborador Convidado',
  email: 'operador@teste.com',
  cpf: '99988877766',
  senhaHash: undefined,
  tipoPerfil: 'USUARIO' as TipoPerfil,
  status: 'PENDENTE_CADASTRO' as StatusUsuario
};

export const SEED_VINCULO_COLABORADOR: EmpresaUsuarioDTO = {
  id: 'vinc-colab-alpha-0003',
  empresaId: 'emp-alpha-uuid-0001',
  usuarioId: 'usr-colaborador-uuid-0003',
  funcao: 'Operador Financeiro',
  statusVinculo: 'PENDENTE' as StatusVinculo,
  aprovadoPor: 'usr-empresario-uuid-0002'
};
