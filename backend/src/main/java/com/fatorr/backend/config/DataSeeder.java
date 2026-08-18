package com.fatorr.backend.config;

import com.fatorr.backend.domain.FaixaTributaria;
import com.fatorr.backend.domain.ParametroTributario;
import com.fatorr.backend.domain.TipoUsuario;
import com.fatorr.backend.domain.Usuario;
import com.fatorr.backend.repository.ParametroTributarioRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;

@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ParametroTributarioRepository parametroRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed Admin User
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario();
            admin.setCpf("00000000000"); // CPF Padrão para admin master
            admin.setSenha("admin123");
            admin.setTipo(TipoUsuario.ADMINISTRADOR_CONTABIL);
            usuarioRepository.save(admin);
            System.out.println("Usuário Admin master criado: CPF 00000000000 / Senha admin123");
        }

        // Seed Parâmetros Tributários (padrão global)
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
}
