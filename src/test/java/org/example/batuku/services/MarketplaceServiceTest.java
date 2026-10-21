package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.Beat;
import org.example.batuku.domain.Notification;
import org.example.batuku.domain.Purchase;
import org.example.batuku.domain.User;
import org.example.batuku.dto.BeatEditRequest;
import org.example.batuku.dto.BeatResponse;
import org.example.batuku.dto.ProducerResponse;
import org.example.batuku.dto.PurchaseRequest;
import org.example.batuku.dto.PurchaseResponse;
import org.example.batuku.repository.BeatRepository;
import org.example.batuku.repository.PurchaseRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Marketplace de beats: compra com tres licencas, regras de exclusividade,
 * edicao e eliminacao, listagem com filtros e ordenacao.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketplaceServiceTest {

    @Mock BeatRepository beatRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Mock NotificationService notificationService;
    @InjectMocks MarketplaceService service;

    private User comprador;
    private User produtor;
    private User admin;
    private Beat beat;

    @BeforeEach
    void setUp() {
        comprador = TestData.user(1L, User.UserRole.FAN);
        produtor = TestData.user(2L, User.UserRole.ARTIST);
        admin = TestData.user(9L, User.UserRole.ADMIN);
        beat = TestData.beat(5L, produtor);

        when(userRepository.findById(1L)).thenReturn(Optional.of(comprador));
        when(userRepository.findById(2L)).thenReturn(Optional.of(produtor));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(beatRepository.findById(5L)).thenReturn(Optional.of(beat));
        when(purchaseRepository.existsByUserIdAndBeatId(1L, 5L)).thenReturn(false);
        when(beatRepository.save(any(Beat.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PurchaseRequest pedido(Long beatId, String licenca) {
        PurchaseRequest r = new PurchaseRequest();
        r.setBeatId(beatId);
        r.setLicenseType(licenca);
        return r;
    }

    private Purchase compraGuardada() {
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(captor.capture());
        return captor.getValue();
    }

    /* ── Compra ─────────────────────────────────────────────────── */

    @ParameterizedTest(name = "licenca \"{0}\" -> {1} EUR ({2})")
    @CsvSource(nullValues = "null", value = {
            "LEASE,30,LEASE", "PREMIUM,60,PREMIUM", "EXCLUSIVE,200,EXCLUSIVE",
            "premium,60,PREMIUM", "null,30,LEASE", "outra,30,OUTRA"
    })
    @DisplayName("O preco da compra depende do tipo de licenca escolhido")
    void precoPorLicenca(String licenca, String precoEsperado, String licencaGuardada) {
        service.comprar(1L, pedido(5L, licenca));

        Purchase p = compraGuardada();
        assertEquals(new BigDecimal(precoEsperado), p.getPrice());
        assertEquals(licencaGuardada, p.getLicenseType());
        assertEquals(comprador, p.getUser());
        assertEquals(beat, p.getBeat());
    }

    @Test
    @DisplayName("Uma compra incrementa o numero de vendas do beat")
    void compraIncrementaVendas() {
        beat.setSales(3);

        service.comprar(1L, pedido(5L, "LEASE"));

        assertEquals(4, beat.getSales().intValue());
        verify(beatRepository).save(beat);
    }

    @Test
    @DisplayName("O produtor e notificado da compra")
    void produtorENotificado() {
        service.comprar(1L, pedido(5L, "LEASE"));

        verify(notificationService).notify(eq(produtor), eq(Notification.NotificationType.BEAT_PURCHASED),
                any(), contains("Beat 5"));
    }

    @Test
    @DisplayName("Um beat vendido em exclusivo ja nao pode ser comprado")
    void beatExclusivoNaoSeCompra() {
        beat.setSoldExclusively(true);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> service.comprar(1L, pedido(5L, "LEASE")));

        assertTrue(e.getMessage().contains("exclusivo"));
        verify(purchaseRepository, never()).save(any(Purchase.class));
        verify(notificationService, never()).notify(any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("O mesmo beat nao pode ser comprado duas vezes pelo mesmo utilizador")
    void naoSeCompraDuasVezes() {
        when(purchaseRepository.existsByUserIdAndBeatId(1L, 5L)).thenReturn(true);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> service.comprar(1L, pedido(5L, "LEASE")));

        assertTrue(e.getMessage().contains("adquirido"));
        verify(purchaseRepository, never()).save(any(Purchase.class));
        assertEquals(0, beat.getSales().intValue());
    }

    @Test
    @DisplayName("Comprar um beat inexistente falha")
    void beatInexistente() {
        when(beatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.comprar(1L, pedido(99L, "LEASE")));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Uma licenca sem preco definido custa zero")
    void licencaSemPrecoCustaZero() {
        beat.setPremiumPrice(null);

        service.comprar(1L, pedido(5L, "PREMIUM"));

        assertEquals(BigDecimal.ZERO, compraGuardada().getPrice());
    }

    /* ── Edicao ─────────────────────────────────────────────────── */

    @Test
    @DisplayName("O produtor edita apenas os campos recebidos")
    void editaApenasCamposRecebidos() {
        beat.setGenre("Funana");
        BeatEditRequest req = new BeatEditRequest();
        req.setTitle("Novo titulo");
        req.setLeasePrice(new BigDecimal("45"));

        service.editarBeat(2L, 5L, req);

        assertEquals("Novo titulo", beat.getTitle());
        assertEquals(new BigDecimal("45"), beat.getLeasePrice());
        assertEquals("Funana", beat.getGenre());                       // intacto
        assertEquals(new BigDecimal("60"), beat.getPremiumPrice());    // intacto
    }

    @Test
    @DisplayName("Outro utilizador nao pode editar o beat")
    void outroNaoEdita() {
        User outro = TestData.user(3L, User.UserRole.ARTIST);
        when(userRepository.findById(3L)).thenReturn(Optional.of(outro));
        BeatEditRequest req = new BeatEditRequest();
        req.setTitle("Roubado");

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.editarBeat(3L, 5L, req));

        assertTrue(e.getMessage().contains("permiss"));
        assertEquals("Beat 5", beat.getTitle());
    }

    @Test
    @DisplayName("O administrador pode editar beats de qualquer produtor")
    void adminEditaQualquerBeat() {
        BeatEditRequest req = new BeatEditRequest();
        req.setTitle("Corrigido pelo admin");

        service.editarBeat(9L, 5L, req);

        assertEquals("Corrigido pelo admin", beat.getTitle());
    }

    @Test
    @DisplayName("Um beat vendido em exclusivo so pode ser editado pelo administrador")
    void exclusivoSoAdminEdita() {
        beat.setSoldExclusively(true);
        BeatEditRequest req = new BeatEditRequest();
        req.setTitle("Tentativa");

        assertThrows(RuntimeException.class, () -> service.editarBeat(2L, 5L, req));
        assertEquals("Beat 5", beat.getTitle());

        service.editarBeat(9L, 5L, req);
        assertEquals("Tentativa", beat.getTitle());
    }

    @Test
    @DisplayName("So o administrador pode marcar um beat como destaque")
    void soAdminDestaca() {
        BeatEditRequest req = new BeatEditRequest();
        req.setIsFeatured(true);

        service.editarBeat(2L, 5L, req);
        assertFalse(beat.isFeatured());

        service.editarBeat(9L, 5L, req);
        assertTrue(beat.isFeatured());
    }

    /* ── Eliminacao ─────────────────────────────────────────────── */

    @Test
    @DisplayName("O produtor elimina o seu beat")
    void produtorElimina() {
        service.eliminarBeat(2L, 5L);

        verify(beatRepository).delete(beat);
    }

    @Test
    @DisplayName("Outro utilizador nao elimina o beat")
    void outroNaoElimina() {
        assertThrows(RuntimeException.class, () -> service.eliminarBeat(3L, 5L));

        verify(beatRepository, never()).delete(any(Beat.class));
    }

    @Test
    @DisplayName("Um beat vendido em exclusivo nao pode ser eliminado")
    void exclusivoNaoSeElimina() {
        beat.setSoldExclusively(true);

        assertThrows(RuntimeException.class, () -> service.eliminarBeat(2L, 5L));

        verify(beatRepository, never()).delete(any(Beat.class));
    }

    /* ── Publicacao ─────────────────────────────────────────────── */

    private MultipartFile ficheiro(boolean vazio) {
        MultipartFile f = mock(MultipartFile.class);
        when(f.isEmpty()).thenReturn(vazio);
        return f;
    }

    private BeatResponse publicar(User quem, MultipartFile audio, boolean destaque) {
        return service.createBeat(quem.getId(), "Titulo", "Funana", 100, "Am", null,
                new BigDecimal("30"), new BigDecimal("60"), new BigDecimal("200"),
                true, destaque, false, audio, null);
    }

    @Test
    @DisplayName("Publicar um beat sem ficheiro de audio e recusado")
    void semAudioERecusado() {
        assertThrows(IllegalArgumentException.class, () -> publicar(produtor, null, false));
        assertThrows(IllegalArgumentException.class, () -> publicar(produtor, ficheiro(true), false));

        verify(beatRepository, never()).save(any(Beat.class));
    }

    @Test
    @DisplayName("Um beat publicado fica guardado com o URL do audio e o cor por omissao")
    void publicaBeat() {
        MultipartFile audio = ficheiro(false);
        when(fileStorageService.store(audio, FileCategory.BEAT_AUDIO)).thenReturn("chave");
        when(fileStorageService.resolveUrl("chave", FileCategory.BEAT_AUDIO)).thenReturn("https://cdn/beat.mp3");

        publicar(produtor, audio, false);

        ArgumentCaptor<Beat> captor = ArgumentCaptor.forClass(Beat.class);
        verify(beatRepository).save(captor.capture());
        assertEquals("https://cdn/beat.mp3", captor.getValue().getAudioUrl());
        assertEquals(produtor, captor.getValue().getProducer());
        assertEquals((2L * 137) % 360, captor.getValue().getHue().longValue());
    }

    @Test
    @DisplayName("O pedido de destaque so e aceite se o produtor for administrador")
    void destaqueSoParaAdmin() {
        MultipartFile audio = ficheiro(false);
        when(fileStorageService.store(any(), any())).thenReturn("k");

        publicar(produtor, audio, true);
        publicar(admin, audio, true);

        ArgumentCaptor<Beat> captor = ArgumentCaptor.forClass(Beat.class);
        verify(beatRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertFalse(captor.getAllValues().get(0).isFeatured());
        assertTrue(captor.getAllValues().get(1).isFeatured());
    }

    /* ── Listagens ──────────────────────────────────────────────── */

    private Beat beatCom(long id, String genero, String lease, int bpm, int vendas, LocalDateTime criado) {
        Beat b = TestData.beat(id, produtor);
        b.setGenre(genero);
        b.setLeasePrice(new BigDecimal(lease));
        b.setBpm(bpm);
        b.setSales(vendas);
        ReflectionTestUtils.setField(b, "createdAt", criado);
        return b;
    }

    private List<Long> ids(List<BeatResponse> lista) {
        return lista.stream().map(BeatResponse::getId).toList();
    }

    private void tresBeats() {
        LocalDateTime agora = LocalDateTime.now();
        Beat a = beatCom(1, "Funana", "50", 120, 2, agora.minusDays(3));
        Beat b = beatCom(2, "Funana", "20", 90, 9, agora.minusDays(1));
        Beat c = beatCom(3, "Trap", "35", 140, 5, agora.minusDays(2));
        when(beatRepository.findAll()).thenReturn(List.of(a, b, c));
        when(beatRepository.findByGenre("Funana")).thenReturn(List.of(a, b));
    }

    @ParameterizedTest(name = "ordenar por {0}")
    @CsvSource({
            "newest,'2,3,1'",
            "popular,'2,3,1'",
            "price_asc,'2,3,1'",
            "price_desc,'1,3,2'",
            "bpm_asc,'2,1,3'"
    })
    @DisplayName("A listagem respeita o criterio de ordenacao")
    void ordenacao(String criterio, String esperado) {
        tresBeats();

        List<Long> obtido = ids(service.listarBeats(null, criterio));

        List<Long> ordem = java.util.Arrays.stream(esperado.split(",")).map(String::trim).map(Long::valueOf).toList();
        assertEquals(ordem, obtido);
    }

    @Test
    @DisplayName("Sem criterio, os beats mais recentes aparecem primeiro")
    void ordenacaoPorOmissao() {
        tresBeats();

        assertEquals(List.of(2L, 3L, 1L), ids(service.listarBeats("Todos", null)));
    }

    @Test
    @DisplayName("O filtro de genero restringe a listagem")
    void filtroDeGenero() {
        tresBeats();

        assertEquals(List.of(2L, 1L), ids(service.listarBeats("Funana", "newest")));
    }

    @Test
    @DisplayName("Os beats vendidos em exclusivo nao aparecem na listagem")
    void exclusivosNaoAparecem() {
        tresBeats();
        Beat vendido = beatCom(4, "Funana", "10", 100, 1, LocalDateTime.now());
        vendido.setSoldExclusively(true);
        when(beatRepository.findAll()).thenReturn(List.of(vendido, beatCom(3, "Trap", "35", 140, 5, LocalDateTime.now())));

        assertEquals(List.of(3L), ids(service.listarBeats(null, null)));
    }

    @Test
    @DisplayName("A listagem converte os precos para o DTO")
    void precosNoDto() {
        when(beatRepository.findAll()).thenReturn(List.of(beat));

        BeatResponse dto = service.listarBeats(null, null).get(0);

        assertEquals(30.0, dto.getPrices().getLease());
        assertEquals(60.0, dto.getPrices().getPremium());
        assertEquals(200.0, dto.getPrices().getExclusive());
        assertEquals("@user2", dto.getProducer());   // mostra o username do produtor, com "@"
    }

    @Test
    @DisplayName("O historico de compras mostra o username do produtor, com \"@\"")
    void historicoMostraUsernameDoProdutor() {
        Purchase compra = new Purchase();
        compra.setUser(comprador);
        compra.setBeat(beat);
        compra.setPrice(new BigDecimal("30"));
        compra.setLicenseType("LEASE");
        when(purchaseRepository.findByUserId(1L)).thenReturn(List.of(compra));

        PurchaseResponse historico = service.listarMinhasCompras(1L).get(0);

        assertEquals("@user2", historico.getProducer());
        assertEquals(new BigDecimal("30"), historico.getPrice());
    }

    @Test
    @DisplayName("A lista de generos comeca por \"Todos\"")
    void listaDeGeneros() {
        when(beatRepository.findDistinctGenres()).thenReturn(List.of("Funana", "Trap"));

        assertEquals(List.of("Todos", "Funana", "Trap"), service.listarGeneros());
    }

    @Test
    @DisplayName("Os produtores sao ordenados pelas vendas e limitados ao pedido")
    void topProdutores() {
        User outro = TestData.user(3L, User.UserRole.ARTIST);
        Beat b1 = beatCom(1, "Funana", "10", 100, 4, LocalDateTime.now());
        Beat b2 = beatCom(2, "Funana", "10", 100, 3, LocalDateTime.now());
        Beat b3 = TestData.beat(3L, outro);
        b3.setSales(10);
        when(beatRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        List<ProducerResponse> top = service.obterTopProdutores(5);

        assertEquals(2, top.size());
        assertEquals(3L, top.get(0).getId());          // 10 vendas
        assertEquals(7L, top.get(1).getSales());       // 4 + 3
        assertEquals(2L, top.get(1).getBeats());
        assertEquals("@user3", top.get(0).getHandle());
        assertEquals(1, service.obterTopProdutores(1).size());
    }

    @Test
    @DisplayName("As estatisticas do produtor somam vendas e reproducoes; sem receita, e zero")
    void estatisticasDoProdutor() {
        Beat b1 = beatCom(1, "Funana", "10", 100, 4, LocalDateTime.now());
        Beat b2 = beatCom(2, "Funana", "10", 100, 3, LocalDateTime.now());
        b1.setPlays(10);
        b2.setPlays(5);
        when(beatRepository.findByProducerIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(b1, b2));
        when(purchaseRepository.sumRevenueByProducerUserId(2L)).thenReturn(null);

        Map<String, Object> stats = service.obterMeusStats(2L);

        assertEquals(2, stats.get("beats"));
        assertEquals(7L, stats.get("sales"));
        assertEquals(15L, stats.get("plays"));
        assertEquals(BigDecimal.ZERO, stats.get("revenue"));
    }
}
