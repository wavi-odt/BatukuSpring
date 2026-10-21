package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.PendingArtistClaim;
import org.example.batuku.domain.PendingRegistration;
import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.domain.UserProvider;
import org.example.batuku.dto.PendingArtistClaimAdminResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.DiscordAccountRepository;
import org.example.batuku.repository.PendingArtistClaimRepository;
import org.example.batuku.repository.PendingRegistrationRepository;
import org.example.batuku.repository.RefreshTokenRepository;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserProviderRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Registo de artistas: submissao do pedido de verificacao (selfie, documento
 * e artista no Spotify) e decisao do administrador (verificar ou duvidoso).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PendingArtistClaimServiceTest {

    @Mock PendingArtistClaimRepository claimRepository;
    @Mock PendingRegistrationRepository pendingRegistrationRepository;
    @Mock ArtistProfileRepository artistProfileRepository;
    @Mock UserRepository userRepository;
    @Mock UserProviderRepository userProviderRepository;
    @Mock RoleRepository roleRepository;
    @Mock FileStorageService storageService;
    @Mock EmailService emailService;
    @Mock SpotifyClient spotifyClient;
    @Mock GamificationService gamificationService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock DiscordAccountRepository discordAccountRepository;
    @InjectMocks PendingArtistClaimService service;

    private static final String TOKEN = "claim-token";

    private PendingRegistration pendente;
    private MultipartFile selfie;
    private MultipartFile documento;

    @BeforeEach
    void setUp() {
        pendente = new PendingRegistration();
        pendente.setEmail("artista@batuku.test");
        pendente.setUsername("artista");
        pendente.setPassword("hash");
        pendente.setName("Artista Teste");
        pendente.setEmailVerified(true);
        pendente.setExpiresAt(LocalDateTime.now().plusDays(1));

        selfie = ficheiro("image/jpeg", 1_000);
        documento = ficheiro("application/pdf", 2_000);

        when(pendingRegistrationRepository.findByClaimToken(TOKEN)).thenReturn(Optional.of(pendente));
        when(claimRepository.existsByEmailAndStatus(anyString(), any())).thenReturn(false);
        when(spotifyClient.getArtist("sp1")).thenReturn(
                new SpotifyClient.SpotifyArtist("sp1", "Nome Spotify", List.of(), null, null, null, 50));
        when(storageService.store(selfie, FileCategory.SELFIE)).thenReturn("chave-selfie");
        when(storageService.store(documento, FileCategory.ID_DOCUMENT)).thenReturn("chave-doc");
    }

    private static MultipartFile ficheiro(String tipo, long tamanho) {
        MultipartFile f = mock(MultipartFile.class);
        when(f.isEmpty()).thenReturn(false);
        when(f.getContentType()).thenReturn(tipo);
        when(f.getSize()).thenReturn(tamanho);
        return f;
    }

    /* ── Submissao do pedido ────────────────────────────────────── */

    @Test
    @DisplayName("Um pedido valido fica pendente, guarda os ficheiros e consome o registo")
    void pedidoValido() {
        service.submitClaim(TOKEN, "sp1", selfie, documento);

        ArgumentCaptor<PendingArtistClaim> captor = ArgumentCaptor.forClass(PendingArtistClaim.class);
        verify(claimRepository).save(captor.capture());
        PendingArtistClaim claim = captor.getValue();
        assertEquals(PendingArtistClaim.ClaimStatus.PENDING, claim.getStatus());
        assertEquals("artista@batuku.test", claim.getEmail());
        assertEquals("sp1", claim.getSpotifyArtistId());
        assertEquals("Nome Spotify", claim.getSpotifyArtistName());
        assertEquals("chave-selfie", claim.getSelfieKey());
        assertEquals("chave-doc", claim.getIdDocumentKey());
        verify(pendingRegistrationRepository).delete(pendente);
        verify(emailService).sendArtistPendingEmailToAddress("artista@batuku.test", "Artista Teste");
    }

    @Test
    @DisplayName("Uma falha no envio do email nao impede o pedido")
    void falhaDeEmailNaoImpedePedido() {
        doThrow(new RuntimeException("SMTP em baixo"))
                .when(emailService).sendArtistPendingEmailToAddress(anyString(), anyString());

        service.submitClaim(TOKEN, "sp1", selfie, documento);

        verify(claimRepository).save(any(PendingArtistClaim.class));
    }

    @Test
    @DisplayName("Um token desconhecido e recusado")
    void tokenInvalido() {
        when(pendingRegistrationRepository.findByClaimToken("outro")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.submitClaim("outro", "sp1", selfie, documento));

        verify(claimRepository, never()).save(any(PendingArtistClaim.class));
    }

    @Test
    @DisplayName("Sem o email verificado, o pedido e recusado")
    void emailNaoVerificado() {
        pendente.setEmailVerified(false);

        assertThrows(IllegalStateException.class,
                () -> service.submitClaim(TOKEN, "sp1", selfie, documento));

        verify(claimRepository, never()).save(any(PendingArtistClaim.class));
    }

    @Test
    @DisplayName("Um registo expirado e recusado e apagado")
    void registoExpirado() {
        pendente.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.submitClaim(TOKEN, "sp1", selfie, documento));

        verify(pendingRegistrationRepository).delete(pendente);
        verify(claimRepository, never()).save(any(PendingArtistClaim.class));
    }

    @Test
    @DisplayName("Nao pode haver dois pedidos pendentes para o mesmo email")
    void pedidoDuplicado() {
        when(claimRepository.existsByEmailAndStatus("artista@batuku.test", PendingArtistClaim.ClaimStatus.PENDING))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.submitClaim(TOKEN, "sp1", selfie, documento));

        verify(claimRepository, never()).save(any(PendingArtistClaim.class));
    }

    @Test
    @DisplayName("A selfie e obrigatoria")
    void selfieObrigatoria() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submitClaim(TOKEN, "sp1", null, documento));

        verify(storageService, never()).store(any(), any());
    }

    @Test
    @DisplayName("So sao aceites imagens ou PDF")
    void tipoDeFicheiroInvalido() {
        MultipartFile texto = ficheiro("text/plain", 100);

        assertThrows(IllegalArgumentException.class,
                () -> service.submitClaim(TOKEN, "sp1", selfie, texto));

        verify(claimRepository, never()).save(any(PendingArtistClaim.class));
    }

    @Test
    @DisplayName("Os ficheiros nao podem exceder 10 MB")
    void ficheiroDemasiadoGrande() {
        MultipartFile grande = ficheiro("image/png", 10L * 1024 * 1024 + 1);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.submitClaim(TOKEN, "sp1", grande, documento));

        assertTrue(e.getMessage().contains("10 MB"));
    }

    @Test
    @DisplayName("Um ficheiro com exatamente 10 MB e aceite")
    void ficheiroNoLimiteEAceite() {
        MultipartFile noLimite = ficheiro("image/png", 10L * 1024 * 1024);
        when(storageService.store(noLimite, FileCategory.SELFIE)).thenReturn("k");

        service.submitClaim(TOKEN, "sp1", noLimite, documento);

        verify(claimRepository).save(any(PendingArtistClaim.class));
    }

    /* ── Decisao do administrador ───────────────────────────────── */

    private User admin;
    private PendingArtistClaim claim;
    private Role roleArtista;

    private void prepararVerificacao() {
        admin = TestData.user(9L, User.UserRole.ADMIN);
        claim = TestData.withId(new PendingArtistClaim(), 5L);
        claim.setEmail("artista@batuku.test");
        claim.setUsername("artista");
        claim.setPasswordHash("hash");
        claim.setName("Artista Teste");
        claim.setSpotifyArtistId("sp1");
        claim.setSpotifyArtistName("Nome Spotify");
        claim.setSelfieKey("chave-selfie");
        claim.setIdDocumentKey("chave-doc");

        roleArtista = new Role();
        roleArtista.setName("ROLE_ARTIST");

        when(claimRepository.findById(5L)).thenReturn(Optional.of(claim));
        when(roleRepository.findByName("ROLE_ARTIST")).thenReturn(Optional.of(roleArtista));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> TestData.withId(inv.getArgument(0), 50L));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artistProfileRepository.findBySpotifyArtistId("sp1")).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Verificar o pedido cria a conta de artista ja ativa e verificada")
    void verificarCriaConta() {
        prepararVerificacao();

        PendingArtistClaimAdminResponse r = service.verify(5L, admin);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User criado = captor.getValue();
        assertEquals(User.UserRole.ARTIST, criado.getUserRole());
        assertTrue(criado.isEnabled());
        assertTrue(criado.isVerified());
        assertEquals("artista@batuku.test", criado.getEmail());
        assertEquals("hash", criado.getPassword());
        assertTrue(criado.getRoles().contains(roleArtista));
        assertEquals("VERIFIED", r.getStatus());
    }

    @Test
    @DisplayName("Verificar o pedido regista quem o reviu e quando")
    void verificarRegistaRevisor() {
        prepararVerificacao();

        service.verify(5L, admin);

        assertEquals(PendingArtistClaim.ClaimStatus.VERIFIED, claim.getStatus());
        assertEquals(admin, claim.getReviewedBy());
        assertNotNull(claim.getReviewedAt());
        verify(claimRepository).save(claim);
    }

    @Test
    @DisplayName("Sem perfil importado, e criado um perfil de artista reclamado")
    void verificarCriaPerfil() {
        prepararVerificacao();

        service.verify(5L, admin);

        ArgumentCaptor<ArtistProfile> captor = ArgumentCaptor.forClass(ArtistProfile.class);
        verify(artistProfileRepository).save(captor.capture());
        ArtistProfile perfil = captor.getValue();
        assertTrue(perfil.isClaimed());
        assertEquals("Nome Spotify", perfil.getName());
        assertEquals("sp1", perfil.getSpotifyArtistId());
        assertEquals(50L, perfil.getUser().getId().longValue());
    }

    @Test
    @DisplayName("Um perfil importado do Spotify e reutilizado em vez de duplicado")
    void verificarReutilizaPerfilImportado() {
        prepararVerificacao();
        ArtistProfile importado = TestData.withId(new ArtistProfile(), 77L);
        importado.setName("Importado");
        when(artistProfileRepository.findBySpotifyArtistId("sp1")).thenReturn(Optional.of(importado));

        service.verify(5L, admin);

        ArgumentCaptor<ArtistProfile> captor = ArgumentCaptor.forClass(ArtistProfile.class);
        verify(artistProfileRepository).save(captor.capture());
        assertSame(importado, captor.getValue());
        assertTrue(importado.isClaimed());
        assertEquals(50L, importado.getUser().getId().longValue());
    }

    @Test
    @DisplayName("Um perfil que ja tem dono nao e reaproveitado")
    void verificarNaoReutilizaPerfilComDono() {
        prepararVerificacao();
        ArtistProfile ocupado = TestData.artistProfile(77L, TestData.user(3L, User.UserRole.ARTIST));
        when(artistProfileRepository.findBySpotifyArtistId("sp1")).thenReturn(Optional.of(ocupado));

        service.verify(5L, admin);

        ArgumentCaptor<ArtistProfile> captor = ArgumentCaptor.forClass(ArtistProfile.class);
        verify(artistProfileRepository).save(captor.capture());
        assertNotEquals(ocupado, captor.getValue());
        assertEquals(3L, ocupado.getUser().getId().longValue());   // continua do dono anterior
    }

    @Test
    @DisplayName("A pontuacao de gamificacao e inicializada para o novo utilizador")
    void verificarInicializaGamificacao() {
        prepararVerificacao();

        service.verify(5L, admin);

        verify(gamificationService).inicializarPontos(any(User.class));
    }

    @Test
    @DisplayName("Registos feitos com Google ou Discord guardam o fornecedor de identidade")
    void verificarGuardaFornecedor() {
        prepararVerificacao();
        claim.setProvider("google");
        claim.setProviderId("g-123");

        service.verify(5L, admin);

        ArgumentCaptor<UserProvider> captor = ArgumentCaptor.forClass(UserProvider.class);
        verify(userProviderRepository).save(captor.capture());
        assertEquals("google", captor.getValue().getProvider());
        assertEquals("g-123", captor.getValue().getProviderId());
    }

    @Test
    @DisplayName("Registos por email e palavra-passe nao criam fornecedor de identidade")
    void verificarSemFornecedor() {
        prepararVerificacao();

        service.verify(5L, admin);

        verify(userProviderRepository, never()).save(any(UserProvider.class));
    }

    @Test
    @DisplayName("Nao se verifica um pedido cujo email ja tem conta")
    void verificarEmailJaExiste() {
        prepararVerificacao();
        when(userRepository.existsByEmail("artista@batuku.test")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.verify(5L, admin));

        verify(userRepository, never()).save(any(User.class));
        assertEquals(PendingArtistClaim.ClaimStatus.PENDING, claim.getStatus());
    }

    @Test
    @DisplayName("Nao se verifica um pedido cujo username ja esta em uso")
    void verificarUsernameJaExiste() {
        prepararVerificacao();
        when(userRepository.existsByUsername("artista")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.verify(5L, admin));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verificar um pedido inexistente falha")
    void verificarPedidoInexistente() {
        prepararVerificacao();
        when(claimRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.verify(404L, admin));
    }

    @Test
    @DisplayName("Uma falha no email de confirmacao nao desfaz a verificacao")
    void verificarComFalhaDeEmail() {
        prepararVerificacao();
        doThrow(new RuntimeException("SMTP em baixo"))
                .when(emailService).sendClaimVerifiedEmail(any(User.class), any(ArtistProfile.class));

        PendingArtistClaimAdminResponse r = service.verify(5L, admin);

        assertEquals("VERIFIED", r.getStatus());
    }

    @Test
    @DisplayName("Marcar como duvidoso regista o revisor, nao cria conta e avisa o candidato")
    void marcarDuvidoso() {
        prepararVerificacao();

        PendingArtistClaimAdminResponse r = service.markDoubtful(5L, admin);

        assertEquals(PendingArtistClaim.ClaimStatus.DOUBTFUL, claim.getStatus());
        assertEquals(admin, claim.getReviewedBy());
        assertEquals("DOUBTFUL", r.getStatus());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService).sendArtistClaimDoubtfulToAddress("artista@batuku.test", "Artista Teste", "Nome Spotify");
    }

    @Test
    @DisplayName("Uma falha no email de duvida nao impede a decisao")
    void duvidosoComFalhaDeEmail() {
        prepararVerificacao();
        doThrow(new RuntimeException("SMTP em baixo"))
                .when(emailService).sendArtistClaimDoubtfulToAddress(anyString(), anyString(), anyString());

        service.markDoubtful(5L, admin);

        assertEquals(PendingArtistClaim.ClaimStatus.DOUBTFUL, claim.getStatus());
    }

    @Test
    @DisplayName("Apenas os pedidos pendentes sao listados para revisao")
    void listaPendentes() {
        prepararVerificacao();
        when(claimRepository.findByStatusOrderByCreatedAtAsc(PendingArtistClaim.ClaimStatus.PENDING))
                .thenReturn(List.of(claim));
        when(storageService.resolveUrl("chave-selfie", FileCategory.SELFIE)).thenReturn("https://cdn/selfie");

        List<PendingArtistClaimAdminResponse> lista = service.listPending();

        assertEquals(1, lista.size());
        assertEquals("https://cdn/selfie", lista.get(0).getSelfieUrl());
        assertEquals("Nome Spotify", lista.get(0).getArtistName());
    }

    /* ── Conversao de uma conta OAuth2 ──────────────────────────── */

    @Test
    @DisplayName("Converter uma conta OAuth2 cria um registo pendente de artista e apaga a conta de fa")
    void conversaoDeContaOAuth() {
        User fan = TestData.user(4L, User.UserRole.FAN);
        UserProvider up = new UserProvider();
        up.setProvider("google");
        up.setProviderId("g-9");
        when(userProviderRepository.findByUserId(4L)).thenReturn(List.of(up));

        String claimToken = service.initFromOAuth2(fan);

        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepository).save(captor.capture());
        PendingRegistration p = captor.getValue();
        assertEquals(claimToken, p.getClaimToken());
        assertEquals("user4@batuku.test", p.getEmail());
        assertEquals("ARTIST", p.getUserRole());
        assertTrue(p.isEmailVerified());
        assertTrue(p.getPassword().startsWith("{OAUTH}"));
        assertEquals("google", p.getProvider());
        assertEquals("g-9", p.getProviderId());
        assertTrue(p.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));

        verify(pendingRegistrationRepository).deleteByEmail("user4@batuku.test");
        verify(gamificationService).removerDadosUtilizador(fan);
        verify(userRepository).delete(fan);
    }

    @Test
    @DisplayName("Sem fornecedor de identidade, o registo pendente fica sem fornecedor")
    void conversaoSemFornecedor() {
        User fan = TestData.user(4L, User.UserRole.FAN);
        when(userProviderRepository.findByUserId(4L)).thenReturn(List.of());

        service.initFromOAuth2(fan);

        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepository).save(captor.capture());
        assertNull(captor.getValue().getProvider());
        assertFalse(captor.getValue().getClaimToken().isBlank());
    }
}
