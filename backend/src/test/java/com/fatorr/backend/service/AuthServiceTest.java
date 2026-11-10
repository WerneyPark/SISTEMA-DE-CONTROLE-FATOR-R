package com.fatorr.backend.service;

import com.fatorr.backend.domain.*;
import com.fatorr.backend.dto.AuthResponseDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.EmpresaUsuarioRepository;
import com.fatorr.backend.repository.OrganizacaoContabilRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import com.fatorr.backend.util.JwtUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private OrganizacaoContabilRepository organizacaoContabilRepository;

    @Mock
    private EmpresaUsuarioRepository empresaUsuarioRepository;

    @Mock
    private EntityManager entityManager;

    @Spy
    private JwtUtils jwtUtils = new JwtUtils();

    private AuthService authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository,
                empresaRepository,
                organizacaoContabilRepository,
                empresaUsuarioRepository,
                jwtUtils,
                entityManager
        );

        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setUuidId("usr-uuid-001");
        usuarioMock.setNome("Contador Teste");
        usuarioMock.setEmail("contador@teste.com");
        usuarioMock.setCpf("11122233344");
        usuarioMock.setSenha("123456");
        usuarioMock.setSenhaHash("123456"); // Fallback para texto puro legado
        usuarioMock.setTipoPerfil(TipoPerfil.CONTABILISTA);
        usuarioMock.setTipo(TipoUsuario.ADMINISTRADOR_CONTABIL);
        usuarioMock.setStatus(StatusUsuario.ATIVO);
        usuarioMock.setOrganizacaoContabilId("org-uuid-001");
    }

    @Test
    void deveEfetuarLoginComSucesso() {
        when(usuarioRepository.findByCpf("11122233344")).thenReturn(Optional.of(usuarioMock));

        AuthResponseDTO response = authService.login("11122233344", "123456");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("usr-uuid-001", response.getUuidId());
        assertEquals("11122233344", response.getCpf());
        assertEquals(TipoPerfil.CONTABILISTA, response.getTipoPerfil());
        assertNotNull(response.getToken());
        assertTrue(jwtUtils.validateToken(response.getToken()));
        assertEquals("11122233344", jwtUtils.extractCpf(response.getToken()));
    }

    @Test
    void deveLancarExcecaoAoFornecerCredencialInvalida() {
        when(usuarioRepository.findByCpf("11122233344")).thenReturn(Optional.of(usuarioMock));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.login("11122233344", "senhaIncorreta")
        );

        assertEquals("Credenciais inválidas", ex.getMessage());
    }

    @Test
    void deveCadastrarContadorComOrganizacaoContabil() {
        AuthService.RegisterContadorRequest request = new AuthService.RegisterContadorRequest(
                "Novo Contador",
                "novo@contabil.com",
                "52998224725", // CPF válido
                "segredo123",
                "Escritório Alfa Contabilidade",
                "12ABC345000199", // CNPJ alfanumérico válido
                "CRC-SP 998877/O"
        );

        when(usuarioRepository.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("novo@contabil.com")).thenReturn(Optional.empty());
        when(organizacaoContabilRepository.findByCnpj("12ABC345000199")).thenReturn(Optional.empty());

        when(organizacaoContabilRepository.save(any(OrganizacaoContabil.class))).thenAnswer(invocation -> {
            OrganizacaoContabil o = invocation.getArgument(0);
            if (o.getId() == null) o.setId("org-uuid-test");
            return o;
        });

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        AuthResponseDTO response = authService.registerContador(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("52998224725", response.getCpf());
        assertEquals(TipoPerfil.CONTABILISTA, response.getTipoPerfil());
        assertEquals(TipoUsuario.ADMINISTRADOR_CONTABIL, response.getTipo());
        assertNotNull(response.getToken());
        assertTrue(jwtUtils.validateToken(response.getToken()));
    }

    @Test
    void deveExigirContadorAoCadastrarEmpresario() {
        AuthService.RegisterEmpresarioRequest request = new AuthService.RegisterEmpresarioRequest(
                "Empresário Silva",
                "empresario@teste.com",
                "52998224725",
                "segredo123",
                "Empresa Silva Ltda",
                "Silva Tech",
                "12ABC345000199",
                null // Sem indicar contador
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.registerEmpresario(request)
        );

        assertTrue(ex.getMessage().contains("indicar o Contador"));
    }

    @Test
    void deveCadastrarEmpresarioIndicandoContador() {
        OrganizacaoContabil org = new OrganizacaoContabil("org-123", "Contabilidade Central", "12ABC345000199", "CRC-SP 123456/O", LocalDateTime.now(), LocalDateTime.now());
        when(organizacaoContabilRepository.findById("org-123")).thenReturn(Optional.of(org));

        AuthService.RegisterEmpresarioRequest request = new AuthService.RegisterEmpresarioRequest(
                "Empresário Silva",
                "empresario@teste.com",
                "52998224725",
                "segredo123",
                "Empresa Silva Ltda",
                "Silva Tech",
                "98XYZ765000102",
                "org-123"
        );

        when(usuarioRepository.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("empresario@teste.com")).thenReturn(Optional.empty());
        when(empresaRepository.findByCnpj("98XYZ765000102")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa e = invocation.getArgument(0);
            e.setId(10L);
            return e;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        AuthResponseDTO response = authService.registerEmpresario(request);

        assertNotNull(response);
        assertEquals(TipoPerfil.EMPRESARIO, response.getTipoPerfil());
        verify(empresaRepository).save(argThat(e -> "org-123".equals(e.getOrganizacaoContabilId())));
    }

    @Test
    void deveContadorCriarEmpresaGerandoVinculoPendenteParaEmpresario() {
        AuthService.CriarEmpresaRequest request = new AuthService.CriarEmpresaRequest(
                "Nova Empresa Clinica",
                "Clinica Saúde",
                "12ABC345000101",
                "2024-01",
                "52998224725", // CPF do empresário
                "Dr. Roberto",
                null
        );

        when(empresaRepository.findByCnpj("12ABC345000101")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Empresa criada = authService.criarEmpresaComVinculo(request, usuarioMock);

        assertNotNull(criada);
        assertEquals("org-uuid-001", criada.getOrganizacaoContabilId());
        verify(empresaUsuarioRepository).save(argThat(v ->
                v.getStatusVinculo() == StatusVinculo.PENDENTE && "Sócio Administrador".equals(v.getFuncao())
        ));
    }

    @Test
    void devePermitirEmpresarioAceitarConviteDeEmpresa() {
        Usuario empresario = new Usuario();
        empresario.setId(5L);
        empresario.setUuidId("usr-empresario-55");

        EmpresaUsuario vinculoPendente = new EmpresaUsuario(
                "vinc-convite-001",
                "emp-uuid-clinica",
                "usr-empresario-55",
                "Sócio Administrador",
                StatusVinculo.PENDENTE,
                "usr-uuid-001",
                null,
                LocalDateTime.now()
        );

        Empresa empresa = new Empresa();
        empresa.setUuidId("emp-uuid-clinica");
        empresa.setRazaoSocial("Clinica Teste");

        when(empresaUsuarioRepository.findById("vinc-convite-001")).thenReturn(Optional.of(vinculoPendente));
        when(empresaRepository.findByUuidId("emp-uuid-clinica")).thenReturn(Optional.of(empresa));

        authService.responderConviteEmpresa("vinc-convite-001", true, empresario);

        assertEquals(StatusVinculo.APROVADO, vinculoPendente.getStatusVinculo());
        assertEquals("usr-empresario-55", vinculoPendente.getAprovadoPor());
        assertNotNull(vinculoPendente.getAprovadoEm());
        verify(empresaUsuarioRepository).save(vinculoPendente);
        verify(usuarioRepository).save(empresario);
        assertTrue(empresario.getEmpresas().contains(empresa));
    }

    @Test
    void deveAprovarSolicitacaoDeVinculoComSucesso() {
        EmpresaUsuario vinculoPendente = new EmpresaUsuario(
                "vinc-uuid-001",
                "emp-uuid-alpha",
                "usr-uuid-colab",
                "Assistente Fiscal",
                StatusVinculo.PENDENTE,
                null,
                null,
                LocalDateTime.now()
        );

        when(empresaUsuarioRepository.findById("vinc-uuid-001")).thenReturn(Optional.of(vinculoPendente));
        when(empresaUsuarioRepository.save(any(EmpresaUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmpresaUsuario atualizado = authService.atualizarStatusVinculo("vinc-uuid-001", StatusVinculo.APROVADO, "contador-uuid-admin");

        assertNotNull(atualizado);
        assertEquals(StatusVinculo.APROVADO, atualizado.getStatusVinculo());
        assertEquals("contador-uuid-admin", atualizado.getAprovadoPor());
        assertNotNull(atualizado.getAprovadoEm());
    }
}
