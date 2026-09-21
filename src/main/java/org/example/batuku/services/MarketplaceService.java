package org.example.batuku.services;

import org.example.batuku.domain.*;
import org.example.batuku.dto.BeatEditRequest;
import org.example.batuku.dto.BeatResponse;
import org.example.batuku.dto.ProducerResponse;
import org.example.batuku.dto.PurchaseRequest;
import org.example.batuku.dto.PurchaseResponse;
import org.example.batuku.repository.*;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.example.batuku.services.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    private static final String[] SHAPES = {"circles", "orbit", "arch", "sun", "triangles", "wave", "stripes", "split"};

    private final BeatRepository     beatRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository     userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public MarketplaceService(BeatRepository beatRepository,
                              PurchaseRepository purchaseRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              NotificationService notificationService) {
        this.beatRepository     = beatRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository     = userRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    /* ── Beats (com filtro de género + ordenação) ────────────────── */

    @Transactional(readOnly = true)
    public List<BeatResponse> listarBeats(String genre, String sort) {
        List<Beat> beats = ((genre == null || genre.equals("Todos"))
                ? beatRepository.findAll()
                : beatRepository.findByGenre(genre))
                .stream().filter(b -> !b.isSoldExclusively()).collect(Collectors.toList());

        Comparator<Beat> comparator = switch (sort == null ? "newest" : sort) {
            case "popular"    -> Comparator.<Beat, Integer>comparing(Beat::getSales).reversed();
            case "price_asc"  -> Comparator.<Beat, BigDecimal>comparing(b -> b.getLeasePrice() != null ? b.getLeasePrice() : BigDecimal.ZERO);
            case "price_desc" -> Comparator.<Beat, BigDecimal>comparing(b -> b.getLeasePrice() != null ? b.getLeasePrice() : BigDecimal.ZERO).reversed();
            case "bpm_asc"    -> Comparator.<Beat, Integer>comparing(b -> b.getBpm() != null ? b.getBpm() : 0);
            default           -> Comparator.<Beat, java.time.LocalDateTime>comparing(
                    Beat::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
        };

        return beats.stream()
                .sorted(comparator)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ── Beat em destaque ────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public Optional<BeatResponse> obterDestaque() {
        List<Beat> featured = beatRepository.findFeaturedOrdered(PageRequest.of(0, 1));
        if (!featured.isEmpty()) return Optional.of(toDto(featured.get(0)));
        List<Beat> popular = beatRepository.findByPlaysDesc(PageRequest.of(0, 1));
        return popular.isEmpty() ? Optional.empty() : Optional.of(toDto(popular.get(0)));
    }

    /* ── Géneros disponíveis ─────────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<String> listarGeneros() {
        List<String> genres = new ArrayList<>();
        genres.add("Todos");
        genres.addAll(beatRepository.findDistinctGenres());
        return genres;
    }

    /* ── Estatísticas globais ────────────────────────────────────── */

    @Transactional(readOnly = true)
    public Map<String, Object> obterStats() {
        return Map.of(
            "beats",     beatRepository.count(),
            "producers", beatRepository.countDistinctProducers(),
            "sold",      purchaseRepository.countTotalSales()
        );
    }

    /* ── Top produtores ──────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<ProducerResponse> obterTopProdutores(int limit) {
        List<Beat> allBeats = beatRepository.findAll();

        Map<Long, List<Beat>> byProducer = allBeats.stream()
                .collect(Collectors.groupingBy(b -> b.getProducer().getId()));

        return byProducer.entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    List<Beat> userBeats = entry.getValue();
                    User user = userBeats.get(0).getProducer();

                    long totalSales = userBeats.stream().mapToLong(Beat::getSales).sum();

                    String name = (user.getName() != null && !user.getName().isBlank())
                            ? user.getName() : user.getUsername();

                    ProducerResponse p = new ProducerResponse();
                    p.setId(userId);
                    p.setName(name);
                    p.setHandle("@" + user.getUsername());
                    p.setGenre("Vários");
                    p.setBeats(userBeats.size());
                    p.setSales(totalSales);
                    p.setRating(4.5 + (userId % 5) * 0.1);
                    p.setHue((int)((userId * 137) % 360));
                    p.setShape(SHAPES[(int)(userId % SHAPES.length)]);
                    p.setImage(user.getAvatarUrl());
                    p.setVerified(false);
                    return p;
                })
                .sorted(Comparator.comparingLong(ProducerResponse::getSales).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /* ── Publicar beat (upload) ──────────────────────────────────── */

    @Transactional
    public BeatResponse createBeat(Long userId, String title, String genre,
                                   Integer bpm, String musicalKey, Integer hue,
                                   BigDecimal leasePrice, BigDecimal premiumPrice, BigDecimal exclusivePrice,
                                   boolean isNew, boolean isFeatured, boolean exclusiveNegotiable,
                                   MultipartFile audio, MultipartFile cover) {
        User producer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro de áudio é obrigatório.");
        }

        String audioKey = fileStorageService.store(audio, FileCategory.BEAT_AUDIO);
        String audioUrl = fileStorageService.resolveUrl(audioKey, FileCategory.BEAT_AUDIO);

        String coverUrl = null;
        if (cover != null && !cover.isEmpty()) {
            String coverKey = fileStorageService.store(cover, FileCategory.BEAT_COVER);
            coverUrl = fileStorageService.resolveUrl(coverKey, FileCategory.BEAT_COVER);
        }

        Beat beat = new Beat();
        beat.setTitle(title);
        beat.setProducer(producer);
        beat.setGenre(genre);
        beat.setBpm(bpm);
        beat.setMusicalKey(musicalKey);
        beat.setHue(hue != null ? hue : (int)((producer.getId() * 137) % 360));
        beat.setLeasePrice(leasePrice);
        beat.setPremiumPrice(premiumPrice);
        beat.setExclusivePrice(exclusivePrice);
        beat.setAudioUrl(audioUrl);
        beat.setCoverUrl(coverUrl);
        beat.setNew(isNew);
        beat.setFeatured(isFeatured);
        beat.setExclusiveNegotiable(exclusiveNegotiable);

        return toDto(beatRepository.save(beat));
    }

    /* ── Beats do produtor autenticado ──────────────────────────── */

    @Transactional(readOnly = true)
    public List<BeatResponse> listarMeusBeats(Long userId) {
        return beatRepository.findByProducerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ── Comprar beat ────────────────────────────────────────────── */

    @Transactional
    public void comprar(Long userId, PurchaseRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
        Beat beat = beatRepository.findById(req.getBeatId())
                .orElseThrow(() -> new RuntimeException("Beat não encontrado."));

        if (beat.isSoldExclusively()) {
            throw new RuntimeException("Este beat já foi vendido em exclusivo.");
        }
        if (purchaseRepository.existsByUserIdAndBeatId(userId, beat.getId())) {
            throw new RuntimeException("Beat já adquirido.");
        }

        String licenseType = (req.getLicenseType() != null ? req.getLicenseType() : "LEASE").toUpperCase();
        BigDecimal price = switch (licenseType) {
            case "PREMIUM"   -> beat.getPremiumPrice()   != null ? beat.getPremiumPrice()   : BigDecimal.ZERO;
            case "EXCLUSIVE" -> beat.getExclusivePrice() != null ? beat.getExclusivePrice() : BigDecimal.ZERO;
            default          -> beat.getLeasePrice()     != null ? beat.getLeasePrice()     : BigDecimal.ZERO;
        };

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setBeat(beat);
        purchase.setPrice(price);
        purchase.setLicenseType(licenseType);
        purchaseRepository.save(purchase);

        beat.setSales(beat.getSales() + 1);
        beatRepository.save(beat);

        String buyerName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : user.getUsername();
        notificationService.notify(
                beat.getProducer(),
                Notification.NotificationType.BEAT_PURCHASED,
                purchase.getId(),
                "\"" + beat.getTitle() + "\" foi comprado por " + buyerName
        );
    }

    /* ── Histórico de compras (fã) ───────────────────────────────── */

    @Transactional(readOnly = true)
    public List<PurchaseResponse> listarMinhasCompras(Long userId) {
        return purchaseRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Purchase::getPurchasedAt).reversed())
                .map(this::toPurchaseDto)
                .collect(Collectors.toList());
    }

    /* ── Estatísticas do produtor (com receita) ──────────────────── */

    @Transactional(readOnly = true)
    public Map<String, Object> obterMeusStats(Long userId) {
        List<BeatResponse> beats = listarMeusBeats(userId);
        long totalSales = beats.stream().mapToLong(b -> b.getSales() != null ? b.getSales() : 0).sum();
        long totalPlays = beats.stream().mapToLong(b -> b.getPlays() != null ? b.getPlays() : 0).sum();
        BigDecimal revenue = purchaseRepository.sumRevenueByProducerUserId(userId);
        return Map.of(
            "beats",   beats.size(),
            "sales",   totalSales,
            "plays",   totalPlays,
            "revenue", revenue != null ? revenue : BigDecimal.ZERO
        );
    }

    /* ── Editar beat (produtor) ──────────────────────────────────── */

    @Transactional
    public BeatResponse editarBeat(Long userId, Long beatId, BeatEditRequest req) {
        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new RuntimeException("Beat não encontrado."));
        if (beat.getProducer() == null || !beat.getProducer().getId().equals(userId)) {
            throw new RuntimeException("Sem permissão para editar este beat.");
        }
        if (req.getTitle()              != null) beat.setTitle(req.getTitle());
        if (req.getGenre()              != null) beat.setGenre(req.getGenre());
        if (req.getBpm()                != null) beat.setBpm(req.getBpm());
        if (req.getMusicalKey()         != null) beat.setMusicalKey(req.getMusicalKey());
        if (req.getHue()                != null) beat.setHue(req.getHue());
        if (req.getLeasePrice()         != null) beat.setLeasePrice(req.getLeasePrice());
        if (req.getPremiumPrice()       != null) beat.setPremiumPrice(req.getPremiumPrice());
        if (req.getExclusivePrice()     != null) beat.setExclusivePrice(req.getExclusivePrice());
        if (req.getIsNew()              != null) beat.setNew(req.getIsNew());
        if (req.getIsFeatured()         != null) beat.setFeatured(req.getIsFeatured());
        if (req.getExclusiveNegotiable()!= null) beat.setExclusiveNegotiable(req.getExclusiveNegotiable());
        return toDto(beatRepository.save(beat));
    }

    /* ── Eliminar beat (produtor) ────────────────────────────────── */

    @Transactional
    public void eliminarBeat(Long userId, Long beatId) {
        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new RuntimeException("Beat não encontrado."));
        if (beat.getProducer() == null || !beat.getProducer().getId().equals(userId)) {
            throw new RuntimeException("Sem permissão para eliminar este beat.");
        }
        beatRepository.delete(beat);
    }

    /* ── Mapeamento Purchase → DTO ───────────────────────────────── */

    private PurchaseResponse toPurchaseDto(Purchase p) {
        User prod = p.getBeat().getProducer();
        String producerName = (prod.getName() != null && !prod.getName().isBlank())
                ? prod.getName() : prod.getUsername();

        PurchaseResponse dto = new PurchaseResponse();
        dto.setId(p.getId());
        dto.setBeatId(p.getBeat().getId());
        dto.setTitle(p.getBeat().getTitle());
        dto.setProducer(producerName);
        dto.setGenre(p.getBeat().getGenre());
        dto.setLicenseType(p.getLicenseType());
        dto.setPrice(p.getPrice());
        dto.setPurchasedAt(p.getPurchasedAt());
        dto.setAudioUrl(p.getBeat().getAudioUrl());
        dto.setCoverUrl(p.getBeat().getCoverUrl());
        dto.setHue(p.getBeat().getHue());
        return dto;
    }

    /* ── Mapeamento Beat → DTO ───────────────────────────────────── */

    private BeatResponse toDto(Beat beat) {
        User prod = beat.getProducer();
        String producerName = (prod.getName() != null && !prod.getName().isBlank())
                ? prod.getName() : prod.getUsername();

        BeatResponse dto = new BeatResponse();
        dto.setId(beat.getId());
        dto.setTitle(beat.getTitle());
        dto.setProducer(producerName);
        dto.setProducerId(prod.getId());
        dto.setGenre(beat.getGenre());
        dto.setBpm(beat.getBpm());
        dto.setKey(beat.getMusicalKey());
        dto.setHue(beat.getHue());
        dto.setImage(beat.getCoverUrl());
        dto.setAudioUrl(beat.getAudioUrl());
        dto.setPlays(beat.getPlays());
        dto.setSales(beat.getSales());
        dto.setNew(beat.isNew());
        dto.setFeatured(beat.isFeatured());
        dto.setExclusiveNegotiable(beat.isExclusiveNegotiable());

        BeatResponse.Prices prices = new BeatResponse.Prices();
        prices.setLease(beat.getLeasePrice()     != null ? beat.getLeasePrice().doubleValue()     : 0.0);
        prices.setPremium(beat.getPremiumPrice()  != null ? beat.getPremiumPrice().doubleValue()   : 0.0);
        prices.setExclusive(beat.getExclusivePrice() != null ? beat.getExclusivePrice().doubleValue() : 0.0);
        dto.setPrices(prices);

        return dto;
    }
}
