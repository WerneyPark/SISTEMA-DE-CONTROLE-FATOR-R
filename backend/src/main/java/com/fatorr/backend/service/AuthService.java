package com.fatorr.backend.service;

import com.fatorr.backend.domain.*;
import com.fatorr.backend.dto.AuthResponseDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.EmpresaUsuarioRepository;
import com.fatorr.backend.repository.OrganizacaoContabilRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import com.fatorr.backend.util.CnpjUtils;
import com.fatorr.backend.util.CpfUtils;
import com.fatorr.backend.util.JwtUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final OrganizacaoContabilRepository organizacaoContabilRepository;
    private final EmpresaUsuarioRepository empresaUsuarioRepository;
    private final JwtUtils jwtUtils;

    @PersistenceContext
    private final EntityManager entityManager;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            OrganizacaoContabilRepository organizacaoContabilRepository,
            EmpresaUsuarioRepository empresaUsuarioRepository,
            JwtUtils jwtUtils,
            EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.organizacaoContabilRepository = organizacaoContabilRepository;
        this.empresaUsuarioRepository = empresaUsuarioRepository;
        this.jwtUtils = jwtUtils;
        this.entityManager = entityManager;
    }

    // ==========================================
    // DTOs Internos para Requisições de Auth
    // ==========================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String login;
        private String cpf;
        private String email;
        private String senha;

        public String getIdentifier() {
            if (login != null && !login.isBlank()) return login.trim();
            if (cpf != null && !cpf.isBlank()) return cpf.trim();
            if (email != null && !email.isBlank()) return email.trim();
            return "";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterContadorRequest {
        private String nome;
        private String email;
        private String cpf;
        private String senha;
        private String nomeOrganizacao;
        private String cnpj;
        private String crc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterEmpresarioRequest {
        private String nome;
        private String email;
        private String cpf;
        private String senha;
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterUsuarioRequest {
        private String nome;
        private String email;
        private String cpf;
        private String senha;
        private String empresaId;
        private String funcao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        private String login;
        private String email;
        private String cpf;

        public String getIdentifier() {
            if (login != null && !login.isBlank()) return login.trim();
            if (email != null && !email.isBlank()) return email.trim();
            if (cpf != null && !cpf.isBlank()) return cpf.trim();
            return "";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        private String token;
        private String novaSenha;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VinculoPendenteDTO {
        private String id;
        private String empresaId;
        private String empresaNome;
        private String empresaCnpj;
        private String usuarioId;
        private String usuarioNome;
        private String usuarioCpf;
        private String usuarioEmail;
        private String funcao;
        private StatusVinculo statusVinculo;
        private LocalDateTime createdAt;
    }

    // ==========================================
    // 1. Login e Autenticação
    // ==========================================
    @Transactional
    public AuthResponseDTO login(String loginIdentifier, String rawPassword) {
        if (loginIdentifier == null || loginIdentifier.isBlank()) {
            throw new IllegalArgumentException("Identificador de login (CPF ou e-mail) obrigatório");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória");
        }

        String trimmed = loginIdentifier.trim();
        Optional<Usuario> usuarioOpt = Optional.empty();

        // 1. Tentar busca por CPF se o formato for numérico/máscara
        String cleanCpf = CpfUtils.unmask(trimmed);
        if (!cleanCpf.isEmpty()) {
            usuarioOpt = usuarioRepository.findByCpf(cleanCpf);
        }

        // 2. Se não encontrou por CPF, buscar por e-mail
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(trimmed.toLowerCase());
        }

        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        Usuario usuario = usuarioOpt.get();

        // 3. Validação da Senha (BCrypt com fallback gracioso para texto puro de seed/testes)
        boolean senhaValida = matchesPassword(rawPassword, usuario.getSenhaHash())
                || matchesPassword(rawPassword, usuario.getSenha());

        if (!senhaValida) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        // Se a senha estava em texto puro, atualiza silenciosamente para hash BCrypt
        if (usuario.getSenhaHash() == null || !usuario.getSenhaHash().startsWith("$2")) {
            String novoHash = passwordEncoder.encode(rawPassword);
            usuario.setSenhaHash(novoHash);
            usuarioRepository.save(usuario);
        }

        // 4. Gerar Token JWT nativo
        String token = jwtUtils.generateToken(usuario);

        // 5. Carregar empresas vinculadas e seus respectivos status de vínculo
        List<AuthResponseDTO.EmpresaVinculadaDTO> empresasVinculadas = carregarEmpresasUsuario(usuario);

        return AuthResponseDTO.builder()
                .id(usuario.getId())
                .uuidId(usuario.getUuidId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .cpf(usuario.getCpf())
                .tipoPerfil(usuario.getTipoPerfil())
                .tipo(usuario.getTipo())
                .status(usuario.getStatus())
                .organizacaoContabilId(usuario.getOrganizacaoContabilId())
                .token(token)
                .empresas(empresasVinculadas)
                .build();
    }

    // ==========================================
    // 2. Registro: Contador
    // ==========================================
    @Transactional
    public AuthResponseDTO registerContador(RegisterContadorRequest request) {
        if (!CnpjUtils.isValid(request.getCnpj())) {
            throw new IllegalArgumentException("CNPJ da Organização Contábil inválido");
        }
        if (request.getCrc() == null || request.getCrc().isBlank()) {
            throw new IllegalArgumentException("CRC da Organização Contábil obrigatório");
        }
        if (!CpfUtils.isValid(request.getCpf())) {
            throw new IllegalArgumentException("CPF do Contador inválido");
        }
        if (request.getSenha() == null || request.getSenha().length() < 6) {
            throw new IllegalArgumentException("A senha deve possuir pelo menos 6 caracteres");
        }

        String cleanCpf = CpfUtils.unmask(request.getCpf());
        String cleanCnpj = CnpjUtils.unmask(request.getCnpj());

        if (usuarioRepository.findByCpf(cleanCpf).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este CPF");
        }
        if (request.getEmail() != null && usuarioRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail");
        }

        // Cadastra ou reutiliza a Organização Contábil
        OrganizacaoContabil org = organizacaoContabilRepository.findByCnpj(cleanCnpj)
                .orElseGet(() -> {
                    OrganizacaoContabil novaOrg = new OrganizacaoContabil();
                    novaOrg.setId(UUID.randomUUID().toString());
                    novaOrg.setNome(request.getNomeOrganizacao() != null && !request.getNomeOrganizacao().isBlank()
                            ? request.getNomeOrganizacao() : "Organização Contábil " + CnpjUtils.mask(cleanCnpj));
                    novaOrg.setCnpj(cleanCnpj);
                    novaOrg.setCrc(request.getCrc());
                    novaOrg.setCreatedAt(LocalDateTime.now());
                    novaOrg.setUpdatedAt(LocalDateTime.now());
                    return organizacaoContabilRepository.save(novaOrg);
                });

        // Cadastra o usuário Contador
        String encodedPass = passwordEncoder.encode(request.getSenha());
        Usuario contador = new Usuario();
        contador.setUuidId(UUID.randomUUID().toString());
        contador.setNome(request.getNome());
        contador.setEmail(request.getEmail() != null ? request.getEmail().toLowerCase() : null);
        contador.setCpf(cleanCpf);
        contador.setSenha(encodedPass);
        contador.setSenhaHash(encodedPass);
        contador.setTipoPerfil(TipoPerfil.CONTABILISTA);
        contador.setTipo(TipoUsuario.ADMINISTRADOR_CONTABIL);
        contador.setOrganizacaoContabilId(org.getId());
        contador.setStatus(StatusUsuario.ATIVO);

        contador = usuarioRepository.save(contador);

        String token = jwtUtils.generateToken(contador);
        List<AuthResponseDTO.EmpresaVinculadaDTO> empresas = carregarEmpresasUsuario(contador);

        return AuthResponseDTO.builder()
                .id(contador.getId())
                .uuidId(contador.getUuidId())
                .nome(contador.getNome())
                .email(contador.getEmail())
                .cpf(contador.getCpf())
                .tipoPerfil(contador.getTipoPerfil())
                .tipo(contador.getTipo())
                .status(contador.getStatus())
                .organizacaoContabilId(contador.getOrganizacaoContabilId())
                .token(token)
                .empresas(empresas)
                .build();
    }

    // ==========================================
    // 3. Registro: Empresário
    // ==========================================
    @Transactional
    public AuthResponseDTO registerEmpresario(RegisterEmpresarioRequest request) {
        if (!CpfUtils.isValid(request.getCpf())) {
            throw new IllegalArgumentException("CPF do Empresário inválido");
        }
        if (!CnpjUtils.isValid(request.getCnpj())) {
            throw new IllegalArgumentException("CNPJ da Empresa inválido");
        }
        if (request.getSenha() == null || request.getSenha().length() < 6) {
            throw new IllegalArgumentException("A senha deve possuir pelo menos 6 caracteres");
        }

        String cleanCpf = CpfUtils.unmask(request.getCpf());
        String cleanCnpj = CnpjUtils.unmask(request.getCnpj());

        if (usuarioRepository.findByCpf(cleanCpf).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este CPF");
        }
        if (request.getEmail() != null && usuarioRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail");
        }

        // Criar ou localizar Empresa
        Empresa empresa = empresaRepository.findByCnpj(cleanCnpj)
                .orElseGet(() -> {
                    Empresa e = new Empresa();
                    e.setUuidId(UUID.randomUUID().toString());
                    e.setRazaoSocial(request.getRazaoSocial() != null && !request.getRazaoSocial().isBlank()
                            ? request.getRazaoSocial() : "Empresa " + cleanCnpj);
                    e.setNomeFantasia(request.getNomeFantasia());
                    e.setCnpj(cleanCnpj);
                    e.setDataInicioAtividade(LocalDate.now());
                    return empresaRepository.save(e);
                });

        // Cadastrar usuário Empresário
        String encodedPass = passwordEncoder.encode(request.getSenha());
        Usuario empresario = new Usuario();
        empresario.setUuidId(UUID.randomUUID().toString());
        empresario.setNome(request.getNome());
        empresario.setEmail(request.getEmail() != null ? request.getEmail().toLowerCase() : null);
        empresario.setCpf(cleanCpf);
        empresario.setSenha(encodedPass);
        empresario.setSenhaHash(encodedPass);
        empresario.setTipoPerfil(TipoPerfil.EMPRESARIO);
        empresario.setTipo(TipoUsuario.USUARIO_EMPRESA);
        empresario.setStatus(StatusUsuario.ATIVO);
        empresario.getEmpresas().add(empresa);

        empresario = usuarioRepository.save(empresario);

        // Criar Vínculo APROVADO em EmpresaUsuario
        Optional<EmpresaUsuario> vincOpt = empresaUsuarioRepository.findByEmpresaIdAndUsuarioId(empresa.getUuidId(), empresario.getUuidId());
        if (vincOpt.isEmpty()) {
            EmpresaUsuario vinc = new EmpresaUsuario(
                    UUID.randomUUID().toString(),
                    empresa.getUuidId(),
                    empresario.getUuidId(),
                    "Sócio Administrador",
                    StatusVinculo.APROVADO,
                    empresario.getUuidId(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            empresaUsuarioRepository.save(vinc);
        }

        String token = jwtUtils.generateToken(empresario);
        List<AuthResponseDTO.EmpresaVinculadaDTO> empresas = carregarEmpresasUsuario(empresario);

        return AuthResponseDTO.builder()
                .id(empresario.getId())
                .uuidId(empresario.getUuidId())
                .nome(empresario.getNome())
                .email(empresario.getEmail())
                .cpf(empresario.getCpf())
                .tipoPerfil(empresario.getTipoPerfil())
                .tipo(empresario.getTipo())
                .status(empresario.getStatus())
                .token(token)
                .empresas(empresas)
                .build();
    }

    // ==========================================
    // 4. Registro: Colaborador (Usuário)
    // ==========================================
    @Transactional
    public AuthResponseDTO registerUsuario(RegisterUsuarioRequest request) {
        if (!CpfUtils.isValid(request.getCpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (request.getSenha() == null || request.getSenha().length() < 6) {
            throw new IllegalArgumentException("A senha deve possuir pelo menos 6 caracteres");
        }

        String cleanCpf = CpfUtils.unmask(request.getCpf());
        Empresa empresa = buscarEmpresaPorIdentificador(request.getEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com identificador: " + request.getEmpresaId()));

        Optional<Usuario> userOpt = usuarioRepository.findByCpf(cleanCpf);
        Usuario usuario;
        String encodedPass = passwordEncoder.encode(request.getSenha());

        if (userOpt.isPresent()) {
            usuario = userOpt.get();
            if (request.getNome() != null && !request.getNome().isBlank()) {
                usuario.setNome(request.getNome());
            }
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                usuario.setEmail(request.getEmail().toLowerCase());
            }
            usuario.setSenha(encodedPass);
            usuario.setSenhaHash(encodedPass);
            usuario.setStatus(StatusUsuario.ATIVO);
            usuario.setTipoPerfil(TipoPerfil.USUARIO);
            usuario.setTipo(TipoUsuario.USUARIO_EMPRESA);
        } else {
            usuario = new Usuario();
            usuario.setUuidId(UUID.randomUUID().toString());
            usuario.setNome(request.getNome());
            usuario.setEmail(request.getEmail() != null ? request.getEmail().toLowerCase() : null);
            usuario.setCpf(cleanCpf);
            usuario.setSenha(encodedPass);
            usuario.setSenhaHash(encodedPass);
            usuario.setTipoPerfil(TipoPerfil.USUARIO);
            usuario.setTipo(TipoUsuario.USUARIO_EMPRESA);
            usuario.setStatus(StatusUsuario.ATIVO);
        }

        usuario.getEmpresas().add(empresa);
        usuario = usuarioRepository.save(usuario);

        // Verificar pré-cadastro pendente
        Optional<EmpresaUsuario> vincOpt = empresaUsuarioRepository.findByEmpresaIdAndUsuarioId(empresa.getUuidId(), usuario.getUuidId());
        if (vincOpt.isPresent()) {
            EmpresaUsuario vinc = vincOpt.get();
            vinc.setStatusVinculo(StatusVinculo.APROVADO);
            vinc.setAprovadoEm(LocalDateTime.now());
            empresaUsuarioRepository.save(vinc);
        } else {
            EmpresaUsuario novoVinc = new EmpresaUsuario(
                    UUID.randomUUID().toString(),
                    empresa.getUuidId(),
                    usuario.getUuidId(),
                    (request.getFuncao() != null && !request.getFuncao().isBlank()) ? request.getFuncao() : "Colaborador",
                    StatusVinculo.PENDENTE,
                    null,
                    null,
                    LocalDateTime.now()
            );
            empresaUsuarioRepository.save(novoVinc);
        }

        String token = jwtUtils.generateToken(usuario);
        List<AuthResponseDTO.EmpresaVinculadaDTO> empresas = carregarEmpresasUsuario(usuario);

        return AuthResponseDTO.builder()
                .id(usuario.getId())
                .uuidId(usuario.getUuidId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .cpf(usuario.getCpf())
                .tipoPerfil(usuario.getTipoPerfil())
                .tipo(usuario.getTipo())
                .status(usuario.getStatus())
                .token(token)
                .empresas(empresas)
                .build();
    }

    // ==========================================
    // 5. Esqueci a Senha e Redefinição
    // ==========================================
    @Transactional
    public Map<String, String> forgotPassword(String loginIdentifier) {
        if (loginIdentifier == null || loginIdentifier.isBlank()) {
            throw new IllegalArgumentException("Informe o CPF ou e-mail cadastrado");
        }

        String trimmed = loginIdentifier.trim();
        Optional<Usuario> usuarioOpt = Optional.empty();

        String cleanCpf = CpfUtils.unmask(trimmed);
        if (!cleanCpf.isEmpty()) {
            usuarioOpt = usuarioRepository.findByCpf(cleanCpf);
        }
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(trimmed.toLowerCase());
        }

        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Nenhum usuário encontrado com o identificador fornecido");
        }

        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString();
        usuario.setResetPasswordToken(token);
        usuario.setResetPasswordExpires(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Token de recuperação gerado com sucesso. Válido por 1 hora.");
        response.put("resetPasswordToken", token);
        return response;
    }

    @Transactional
    public void resetPassword(String token, String novaSenha) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token de recuperação obrigatório");
        }
        if (novaSenha == null || novaSenha.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve possuir pelo menos 6 caracteres");
        }

        if (entityManager == null) {
            throw new IllegalStateException("EntityManager não disponível");
        }

        List<Usuario> list = entityManager.createQuery(
                "SELECT u FROM Usuario u WHERE u.resetPasswordToken = :token", Usuario.class)
                .setParameter("token", token.trim())
                .getResultList();

        if (list.isEmpty()) {
            throw new IllegalArgumentException("Token inválido ou não encontrado");
        }

        Usuario usuario = list.get(0);
        if (usuario.getResetPasswordExpires() == null || usuario.getResetPasswordExpires().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token de recuperação expirado");
        }

        String encodedPass = passwordEncoder.encode(novaSenha);
        usuario.setSenha(encodedPass);
        usuario.setSenhaHash(encodedPass);
        usuario.setResetPasswordToken(null);
        usuario.setResetPasswordExpires(null);
        usuarioRepository.save(usuario);
    }

    // ==========================================
    // 6. Gestão de Vínculos e Convites
    // ==========================================
    @Transactional
    public EmpresaUsuario convidarPorCpf(String cpf, String funcao, String empresaIdIdentificador) {
        String cleanCpf = CpfUtils.unmask(cpf);
        if (!CpfUtils.isValid(cleanCpf)) {
            throw new IllegalArgumentException("CPF inválido: " + cpf);
        }

        Empresa empresa = buscarEmpresaPorIdentificador(empresaIdIdentificador)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + empresaIdIdentificador));

        Usuario usuario = usuarioRepository.findByCpf(cleanCpf).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setUuidId(UUID.randomUUID().toString());
            u.setNome("Convidado (" + CpfUtils.mask(cleanCpf) + ")");
            u.setCpf(cleanCpf);
            u.setSenha("");
            u.setTipoPerfil(TipoPerfil.USUARIO);
            u.setTipo(TipoUsuario.USUARIO_EMPRESA);
            u.setStatus(StatusUsuario.PENDENTE_CADASTRO);
            return usuarioRepository.save(u);
        });

        Optional<EmpresaUsuario> vincOpt = empresaUsuarioRepository.findByEmpresaIdAndUsuarioId(empresa.getUuidId(), usuario.getUuidId());
        if (vincOpt.isPresent()) {
            return vincOpt.get();
        }

        EmpresaUsuario novoVinculo = new EmpresaUsuario(
                UUID.randomUUID().toString(),
                empresa.getUuidId(),
                usuario.getUuidId(),
                (funcao != null && !funcao.isBlank()) ? funcao : "Colaborador",
                StatusVinculo.PENDENTE,
                null,
                null,
                LocalDateTime.now()
        );

        usuario.getEmpresas().add(empresa);
        usuarioRepository.save(usuario);

        return empresaUsuarioRepository.save(novoVinculo);
    }

    @Transactional(readOnly = true)
    public List<VinculoPendenteDTO> listarVinculosPendentes(String usuarioContextUuid) {
        if (entityManager == null) {
            return Collections.emptyList();
        }

        List<EmpresaUsuario> todosPendentes = entityManager.createQuery(
                "SELECT v FROM EmpresaUsuario v WHERE v.statusVinculo = :status", EmpresaUsuario.class)
                .setParameter("status", StatusVinculo.PENDENTE)
                .getResultList();

        Set<String> empresasPermitidas = new HashSet<>();
        if (usuarioContextUuid != null && !usuarioContextUuid.isBlank()) {
            buscarUsuarioPorUuid(usuarioContextUuid).ifPresent(u -> {
                if (u.getTipoPerfil() == TipoPerfil.CONTABILISTA || u.getTipo() == TipoUsuario.ADMINISTRADOR_CONTABIL) {
                    if (u.getOrganizacaoContabilId() != null) {
                        List<Empresa> emps = entityManager.createQuery(
                                "SELECT e FROM Empresa e WHERE e.organizacaoContabilId = :orgId", Empresa.class)
                                .setParameter("orgId", u.getOrganizacaoContabilId())
                                .getResultList();
                        for (Empresa e : emps) {
                            empresasPermitidas.add(e.getUuidId());
                        }
                    }
                } else {
                    List<EmpresaUsuario> meusVinculos = empresaUsuarioRepository.findByUsuarioId(u.getUuidId());
                    for (EmpresaUsuario v : meusVinculos) {
                        if (v.getStatusVinculo() == StatusVinculo.APROVADO) {
                            empresasPermitidas.add(v.getEmpresaId());
                        }
                    }
                }
            });
        }

        List<VinculoPendenteDTO> resultado = new ArrayList<>();
        for (EmpresaUsuario v : todosPendentes) {
            if (!empresasPermitidas.isEmpty() && !empresasPermitidas.contains(v.getEmpresaId())) {
                continue;
            }

            Optional<Empresa> empOpt = empresaRepository.findByUuidId(v.getEmpresaId());
            Optional<Usuario> usrOpt = buscarUsuarioPorUuid(v.getUsuarioId());

            resultado.add(VinculoPendenteDTO.builder()
                    .id(v.getId())
                    .empresaId(v.getEmpresaId())
                    .empresaNome(empOpt.map(Empresa::getRazaoSocial).orElse("Empresa"))
                    .empresaCnpj(empOpt.map(Empresa::getCnpj).orElse(""))
                    .usuarioId(v.getUsuarioId())
                    .usuarioNome(usrOpt.map(Usuario::getNome).orElse("Usuário"))
                    .usuarioCpf(usrOpt.map(Usuario::getCpf).orElse(""))
                    .usuarioEmail(usrOpt.map(Usuario::getEmail).orElse(""))
                    .funcao(v.getFuncao())
                    .statusVinculo(v.getStatusVinculo())
                    .createdAt(v.getCreatedAt())
                    .build());
        }

        return resultado;
    }

    @Transactional
    public EmpresaUsuario atualizarStatusVinculo(String vinculoId, StatusVinculo novoStatus, String aprovadorUuid) {
        EmpresaUsuario vinc = empresaUsuarioRepository.findById(vinculoId)
                .orElseThrow(() -> new IllegalArgumentException("Vínculo não encontrado com ID: " + vinculoId));

        vinc.setStatusVinculo(novoStatus);
        if (novoStatus == StatusVinculo.APROVADO) {
            vinc.setAprovadoPor(aprovadorUuid);
            vinc.setAprovadoEm(LocalDateTime.now());

            buscarUsuarioPorUuid(vinc.getUsuarioId()).ifPresent(u -> {
                if (u.getStatus() == StatusUsuario.PENDENTE_CADASTRO) {
                    u.setStatus(StatusUsuario.ATIVO);
                }
                buscarEmpresaPorUuid(vinc.getEmpresaId()).ifPresent(e -> u.getEmpresas().add(e));
                usuarioRepository.save(u);
            });
        }

        return empresaUsuarioRepository.save(vinc);
    }

    // ==========================================
    // Utilitários de Consulta e Suporte
    // ==========================================
    public Optional<Usuario> buscarUsuarioPorUuid(String uuidId) {
        if (uuidId == null || uuidId.isBlank() || entityManager == null) return Optional.empty();
        try {
            List<Usuario> list = entityManager.createQuery(
                    "SELECT u FROM Usuario u WHERE u.uuidId = :uuidId", Usuario.class)
                    .setParameter("uuidId", uuidId)
                    .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Empresa> buscarEmpresaPorUuid(String uuidId) {
        if (uuidId == null || uuidId.isBlank()) return Optional.empty();
        return empresaRepository.findByUuidId(uuidId);
    }

    public Optional<Empresa> buscarEmpresaPorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) return Optional.empty();
        String trimmed = identificador.trim();

        // 1. Tentar por ID numérico
        try {
            Long numId = Long.parseLong(trimmed);
            Optional<Empresa> byId = empresaRepository.findById(numId);
            if (byId.isPresent()) return byId;
        } catch (NumberFormatException ignored) {}

        // 2. Tentar por UUID
        Optional<Empresa> byUuid = empresaRepository.findByUuidId(trimmed);
        if (byUuid.isPresent()) return byUuid;

        // 3. Tentar por CNPJ
        String cleanCnpj = CnpjUtils.unmask(trimmed);
        if (!cleanCnpj.isEmpty()) {
            return empresaRepository.findByCnpj(cleanCnpj);
        }

        return Optional.empty();
    }

    private List<AuthResponseDTO.EmpresaVinculadaDTO> carregarEmpresasUsuario(Usuario usuario) {
        Map<String, AuthResponseDTO.EmpresaVinculadaDTO> map = new LinkedHashMap<>();

        // 1. Se for Contador, todas as empresas da sua Organização Contábil
        if ((usuario.getTipoPerfil() == TipoPerfil.CONTABILISTA || usuario.getTipo() == TipoUsuario.ADMINISTRADOR_CONTABIL)
                && usuario.getOrganizacaoContabilId() != null && entityManager != null) {
            try {
                List<Empresa> empresasOrg = entityManager.createQuery(
                        "SELECT e FROM Empresa e WHERE e.organizacaoContabilId = :orgId", Empresa.class)
                        .setParameter("orgId", usuario.getOrganizacaoContabilId())
                        .getResultList();

                for (Empresa emp : empresasOrg) {
                    map.put(emp.getUuidId(), AuthResponseDTO.EmpresaVinculadaDTO.builder()
                            .id(emp.getId())
                            .uuidId(emp.getUuidId())
                            .razaoSocial(emp.getRazaoSocial())
                            .nomeFantasia(emp.getNomeFantasia())
                            .cnpj(emp.getCnpj())
                            .statusVinculo(StatusVinculo.APROVADO)
                            .funcao("Contador Responsável")
                            .build());
                }
            } catch (Exception ignored) {}
        }

        // 2. Vínculos cadastrados em EmpresaUsuario
        if (usuario.getUuidId() != null) {
            List<EmpresaUsuario> vinculos = empresaUsuarioRepository.findByUsuarioId(usuario.getUuidId());
            if (vinculos != null) {
                for (EmpresaUsuario v : vinculos) {
                    Optional<Empresa> empOpt = empresaRepository.findByUuidId(v.getEmpresaId());
                    empOpt.ifPresent(emp -> {
                        map.put(emp.getUuidId(), AuthResponseDTO.EmpresaVinculadaDTO.builder()
                                .id(emp.getId())
                                .uuidId(emp.getUuidId())
                                .razaoSocial(emp.getRazaoSocial())
                                .nomeFantasia(emp.getNomeFantasia())
                                .cnpj(emp.getCnpj())
                                .statusVinculo(v.getStatusVinculo())
                                .funcao(v.getFuncao())
                                .build());
                    });
                }
            }
        }

        // 3. Empresas da relação ManyToMany legada
        if (usuario.getEmpresas() != null) {
            for (Empresa emp : usuario.getEmpresas()) {
                if (emp.getUuidId() != null && !map.containsKey(emp.getUuidId())) {
                    map.put(emp.getUuidId(), AuthResponseDTO.EmpresaVinculadaDTO.builder()
                            .id(emp.getId())
                            .uuidId(emp.getUuidId())
                            .razaoSocial(emp.getRazaoSocial())
                            .nomeFantasia(emp.getNomeFantasia())
                            .cnpj(emp.getCnpj())
                            .statusVinculo(StatusVinculo.APROVADO)
                            .funcao("Colaborador")
                            .build());
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            try {
                if (passwordEncoder.matches(rawPassword, storedPassword)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return rawPassword.equals(storedPassword);
    }
}
