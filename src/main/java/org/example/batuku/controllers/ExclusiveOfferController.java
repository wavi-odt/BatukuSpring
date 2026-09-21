package org.example.batuku.controllers;

import org.example.batuku.domain.*;
import org.example.batuku.dto.OfferReplyRequest;
import org.example.batuku.dto.OfferRequest;
import org.example.batuku.dto.OfferResponse;
import org.example.batuku.repository.BeatRepository;
import org.example.batuku.repository.ExclusiveOfferRepository;
import org.example.batuku.repository.PurchaseRepository;
import org.example.batuku.services.NotificationService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketplace/offers")
public class ExclusiveOfferController {

    private final ExclusiveOfferRepository offerRepository;
    private final BeatRepository           beatRepository;
    private final PurchaseRepository       purchaseRepository;
    private final NotificationService      notificationService;
    private final JwtUserDetailsService    jwtUserDetailsService;

    public ExclusiveOfferController(ExclusiveOfferRepository offerRepository,
                                    BeatRepository beatRepository,
                                    PurchaseRepository purchaseRepository,
                                    NotificationService notificationService,
                                    JwtUserDetailsService jwtUserDetailsService) {
        this.offerRepository      = offerRepository;
        this.beatRepository       = beatRepository;
        this.purchaseRepository   = purchaseRepository;
        this.notificationService  = notificationService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    /* ── Fan submete proposta ─────────────────────────────────────────── */

    @PostMapping
    public ResponseEntity<?> submeterOferta(
            @RequestBody OfferRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        User fan = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        Beat beat = beatRepository.findById(req.getBeatId())
                .orElseThrow(() -> new RuntimeException("Beat não encontrado."));

        if (!beat.isExclusiveNegotiable()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Este beat não aceita propostas."));
        }
        if (beat.isSoldExclusively()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Este beat já foi vendido em exclusivo."));
        }
        if (offerRepository.existsByFanIdAndBeatIdAndStatus(fan.getId(), beat.getId(), "PENDING")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Já tens uma proposta pendente para este beat."));
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valor inválido."));
        }

        ExclusiveOffer offer = new ExclusiveOffer();
        offer.setFan(fan);
        offer.setBeat(beat);
        offer.setAmount(req.getAmount());
        offer.setMessage(req.getMessage());
        offerRepository.save(offer);

        String fanName = (fan.getName() != null && !fan.getName().isBlank()) ? fan.getName() : fan.getUsername();
        notificationService.notify(
                beat.getProducer(),
                Notification.NotificationType.OFFER_RECEIVED,
                offer.getId(),
                fanName + " fez uma proposta de €" + req.getAmount() + " para \"" + beat.getTitle() + "\""
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(offer));
    }

    /* ── Fan vê as suas propostas ─────────────────────────────────────── */

    @GetMapping("/mine")
    public List<OfferResponse> minhasOfertas(
            @AuthenticationPrincipal UserDetails userDetails) {
        User fan = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return offerRepository.findByFanIdOrderByCreatedAtDesc(fan.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /* ── Produtor vê propostas recebidas ─────────────────────────────── */

    @GetMapping("/received")
    public List<OfferResponse> ofertasRecebidas(
            @AuthenticationPrincipal UserDetails userDetails) {
        User producer = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return offerRepository.findByProducerIdOrderByCreatedAtDesc(producer.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /* ── Produtor responde (aceita / recusa) ─────────────────────────── */

    @PatchMapping("/{id}")
    public ResponseEntity<?> responderOferta(
            @PathVariable Long id,
            @RequestBody OfferReplyRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        User producer = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        ExclusiveOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada."));

        if (!offer.getBeat().getProducer().getId().equals(producer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sem permissão."));
        }
        if (!offer.getStatus().equals("PENDING")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Esta proposta já foi respondida."));
        }

        String newStatus = req.getStatus() != null ? req.getStatus().toUpperCase() : "";
        if (!newStatus.equals("ACCEPTED") && !newStatus.equals("REJECTED")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido."));
        }

        offer.setStatus(newStatus);
        offer.setProducerReply(req.getReply());
        offer.setRepliedAt(LocalDateTime.now());
        offerRepository.save(offer);

        Beat beat = offer.getBeat();

        if (newStatus.equals("ACCEPTED")) {
            Purchase purchase = new Purchase();
            purchase.setUser(offer.getFan());
            purchase.setBeat(beat);
            purchase.setPrice(offer.getAmount());
            purchase.setLicenseType("EXCLUSIVE");
            purchaseRepository.save(purchase);

            beat.setSales(beat.getSales() + 1);
            beat.setSoldExclusively(true);
            beatRepository.save(beat);

            /* Rejeitar automaticamente as restantes propostas pendentes */
            offerRepository.findByBeatIdAndStatus(beat.getId(), "PENDING")
                    .forEach(o -> {
                        o.setStatus("REJECTED");
                        offerRepository.save(o);
                        notificationService.notify(
                                o.getFan(),
                                Notification.NotificationType.OFFER_REJECTED,
                                o.getId(),
                                "A tua proposta para \"" + beat.getTitle() + "\" foi recusada."
                        );
                    });

            notificationService.notify(
                    offer.getFan(),
                    Notification.NotificationType.OFFER_ACCEPTED,
                    offer.getId(),
                    "A tua proposta para \"" + beat.getTitle() + "\" foi aceite!"
            );
        } else {
            notificationService.notify(
                    offer.getFan(),
                    Notification.NotificationType.OFFER_REJECTED,
                    offer.getId(),
                    "A tua proposta para \"" + beat.getTitle() + "\" foi recusada."
            );
        }

        return ResponseEntity.ok(toDto(offer));
    }

    /* ── Mapeamento ──────────────────────────────────────────────────── */

    private OfferResponse toDto(ExclusiveOffer o) {
        User fan      = o.getFan();
        User producer = o.getBeat().getProducer();
        String fanName      = (fan.getName()      != null && !fan.getName().isBlank())      ? fan.getName()      : fan.getUsername();
        String producerName = (producer.getName() != null && !producer.getName().isBlank()) ? producer.getName() : producer.getUsername();

        OfferResponse dto = new OfferResponse();
        dto.setId(o.getId());
        dto.setBeatId(o.getBeat().getId());
        dto.setBeatTitle(o.getBeat().getTitle());
        dto.setBeatCoverUrl(o.getBeat().getCoverUrl());
        dto.setBeatHue(o.getBeat().getHue());
        dto.setFanId(fan.getId());
        dto.setFanName(fanName);
        dto.setFanHandle("@" + fan.getUsername());
        dto.setFanAvatarUrl(fan.getAvatarUrl());
        dto.setProducerId(producer.getId());
        dto.setProducerName(producerName);
        dto.setAmount(o.getAmount());
        dto.setMessage(o.getMessage());
        dto.setStatus(o.getStatus());
        dto.setProducerReply(o.getProducerReply());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setRepliedAt(o.getRepliedAt());
        return dto;
    }
}
