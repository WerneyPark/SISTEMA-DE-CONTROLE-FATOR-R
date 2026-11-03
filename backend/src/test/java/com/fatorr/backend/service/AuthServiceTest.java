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
