package com.fatorr.backend.config;

import com.fatorr.backend.domain.*;
import com.fatorr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final OrganizacaoContabilRepository organizacaoContabilRepository;
    private final EmpresaUsuarioRepository empresaUsuarioRepository;
    private final ParametroTributarioRepository parametroRepository;

    @Override
    public void run(String... args) throws Exception {
        seedParametrosTributarios();
        seedSprint0Data();
    }

    private void seedParametrosTributarios() {
        if (parametroRepository.findByAtivoTrueAndEmpresaIsNull().isEmpty()) {
            ParametroTributario params = new ParametroTributario();
            params.setAliquotaPis(new BigDecimal("0.0065"));
            params.setAliquotaCofins(new BigDecimal("0.0300"));
            params.setAliquotaIss(new BigDecimal("0.0500"));
            params.setAliquotaIrpj(new BigDecimal("0.1500"));
            params.setAliquotaAdicionalIrpj(new BigDecimal("0.1000"));
            params.setAliquotaCsll(new BigDecimal("0.0900"));
            params.setAtivo(true);
            params.setFaixas(new ArrayList<>());

            // Anexo III
            params.getFaixas().add(new FaixaTributaria(null, "III", 1, new BigDecimal("180000.00"), new BigDecimal("0.0600"), new BigDecimal("0.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "III", 2, new BigDecimal("360000.00"), new BigDecimal("0.1120"), new BigDecimal("9360.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "III", 3, new BigDecimal("720000.00"), new BigDecimal("0.1350"), new BigDecimal("17640.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "III", 4, new BigDecimal("1800000.00"), new BigDecimal("0.1600"), new BigDecimal("35640.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "III", 5, new BigDecimal("3600000.00"), new BigDecimal("0.2100"), new BigDecimal("125640.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "III", 6, null, new BigDecimal("0.3300"), new BigDecimal("648000.00"), params));

            // Anexo V
            params.getFaixas().add(new FaixaTributaria(null, "V", 1, new BigDecimal("180000.00"), new BigDecimal("0.1550"), new BigDecimal("0.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "V", 2, new BigDecimal("360000.00"), new BigDecimal("0.1800"), new BigDecimal("4500.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "V", 3, new BigDecimal("720000.00"), new BigDecimal("0.1950"), new BigDecimal("9900.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "V", 4, new BigDecimal("1800000.00"), new BigDecimal("0.2050"), new BigDecimal("17100.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "V", 5, new BigDecimal("3600000.00"), new BigDecimal("0.2300"), new BigDecimal("62100.00"), params));
            params.getFaixas().add(new FaixaTributaria(null, "V", 6, null, new BigDecimal("0.3050"), new BigDecimal("540000.00"), params));

            parametroRepository.save(params);
            System.out.println("Parâmetros Tributários e Faixas padrões criados.");
        }
    }

    private void seedSprint0Data() {
        // 1. Organização Contábil
        OrganizacaoContabil org = organizacaoContabilRepository.findById("org-modelo-uuid-0001")
            .orElseGet(() -> {
                OrganizacaoContabil novaOrg = new OrganizacaoContabil(
                    "org-modelo-uuid-0001",
                    "Organização Contábil Modelo Ltda",
                    "12ABC345000199",
                    "CRC-SP 123456/O",
                    LocalDateTime.now(),
                    LocalDateTime.now()
                );
                return organizacaoContabilRepository.save(novaOrg);
            });

        // 2. Usuário Contabilista (Admin Master)
        Usuario contador = usuarioRepository.findByCpf("111.222.333-44")
            .orElseGet(() -> {
                Usuario u = new Usuario();
                u.setUuidId("usr-contador-uuid-0001");
                u.setNome("Contador Chefe");
                u.setEmail("contador@teste.com");
                u.setCpf("111.222.333-44");
                u.setSenha("123456");
                u.setSenhaHash("123456");
                u.setTipoPerfil(TipoPerfil.CONTABILISTA);
                u.setTipo(TipoUsuario.ADMINISTRADOR_CONTABIL);
                u.setOrganizacaoContabilId(org.getId());
                u.setStatus(StatusUsuario.ATIVO);
                return usuarioRepository.save(u);
            });

        // 3. Duas Empresas Vinculadas à Contabilidade
        Empresa empAlpha = empresaRepository.findByCnpj("12ABC345000101")
            .orElseGet(() -> {
                Empresa e = new Empresa();
                e.setUuidId("emp-alpha-uuid-0001");
                e.setRazaoSocial("Empresa Alpha Serviços");
                e.setNomeFantasia("Alpha Serviços Contábeis");
                e.setCnpj("12ABC345000101");
                e.setOrganizacaoContabilId(org.getId());
                e.setDataInicioAtividade(LocalDate.of(2024, 1, 1));
                return empresaRepository.save(e);
            });

        Empresa empBeta = empresaRepository.findByCnpj("98XYZ765000102")
            .orElseGet(() -> {
                Empresa e = new Empresa();
                e.setUuidId("emp-beta-uuid-0002");
                e.setRazaoSocial("Empresa Beta Tecnologia");
                e.setNomeFantasia("Beta Tech Solutions");
                e.setCnpj("98XYZ765000102");
                e.setOrganizacaoContabilId(org.getId());
                e.setDataInicioAtividade(LocalDate.of(2024, 1, 1));
                return empresaRepository.save(e);
            });

        // 4. Usuário Empresário
        Usuario empresario = usuarioRepository.findByCpf("222.333.444-55")
            .orElseGet(() -> {
                Usuario u = new Usuario();
                u.setUuidId("usr-empresario-uuid-0002");
                u.setNome("Empresário Silva");
                u.setEmail("empresario@teste.com");
                u.setCpf("222.333.444-55");
                u.setSenha("123456");
                u.setSenhaHash("123456");
                u.setTipoPerfil(TipoPerfil.EMPRESARIO);
                u.setTipo(TipoUsuario.USUARIO_EMPRESA);
                u.setStatus(StatusUsuario.ATIVO);
                u.getEmpresas().add(empAlpha);
                u.getEmpresas().add(empBeta);
                return usuarioRepository.save(u);
            });

        // Vínculos NxN do Empresário
        if (empresaUsuarioRepository.findById("vinc-emp-alpha-0001").isEmpty()) {
            empresaUsuarioRepository.save(new EmpresaUsuario(
                "vinc-emp-alpha-0001",
                empAlpha.getUuidId(),
                empresario.getUuidId(),
                "Sócio Administrador",
                StatusVinculo.APROVADO,
                contador.getUuidId(),
                LocalDateTime.now(),
                LocalDateTime.now()
            ));
        }

        if (empresaUsuarioRepository.findById("vinc-emp-beta-0002").isEmpty()) {
            empresaUsuarioRepository.save(new EmpresaUsuario(
                "vinc-emp-beta-0002",
                empBeta.getUuidId(),
                empresario.getUuidId(),
                "Sócio Diretor",
                StatusVinculo.APROVADO,
                contador.getUuidId(),
                LocalDateTime.now(),
                LocalDateTime.now()
            ));
        }

        // 5. Usuário Colaborador Pré-Cadastrado por CPF
        Usuario colaborador = usuarioRepository.findByCpf("999.888.777-66")
            .orElseGet(() -> {
                Usuario u = new Usuario();
                u.setUuidId("usr-colaborador-uuid-0003");
                u.setNome("Colaborador Convidado");
                u.setEmail("operador@teste.com");
                u.setCpf("999.888.777-66");
                u.setSenha(""); // Vazia/Pendente de definição pelo convidado
                u.setTipoPerfil(TipoPerfil.USUARIO);
                u.setTipo(TipoUsuario.USUARIO_EMPRESA);
                u.setStatus(StatusUsuario.PENDENTE_CADASTRO);
                return usuarioRepository.save(u);
            });

        // Vínculo do Colaborador na Empresa Alpha
        if (empresaUsuarioRepository.findById("vinc-colab-alpha-0003").isEmpty()) {
            empresaUsuarioRepository.save(new EmpresaUsuario(
                "vinc-colab-alpha-0003",
                empAlpha.getUuidId(),
                colaborador.getUuidId(),
                "Operador Financeiro",
                StatusVinculo.PENDENTE,
                empresario.getUuidId(),
                LocalDateTime.now(),
                LocalDateTime.now()
            ));
        }

        System.out.println("Seeds Sprint 0 executadas com sucesso.");
    }
}
