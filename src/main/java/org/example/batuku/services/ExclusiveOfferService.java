package org.example.batuku.services;

import org.example.batuku.domain.Beat;
import org.example.batuku.domain.ExclusiveOffer;
import org.example.batuku.domain.Notification;
import org.example.batuku.domain.Purchase;
import org.example.batuku.domain.User;
import org.example.batuku.dto.OfferReplyRequest;
import org.example.batuku.dto.OfferRequest;
import org.example.batuku.dto.OfferResponse;
import org.example.batuku.repository.BeatRepository;
import org.example.batuku.repository.ExclusiveOfferRepository;
import org.example.batuku.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExclusiveOfferService {

    private final ExclusiveOfferRepository offerRepository;
    private final BeatRepository           beatRepository;
    private final PurchaseRepository       purchaseRepository;
    private final NotificationService      notificationService;

    public ExclusiveOfferService(ExclusiveOfferRepository offerRepository,
                                 BeatRepository beatRepository,
                                 PurchaseRepository purchaseRepository,
                                 NotificationService notificationService) {
        this.offerRepository     = offerRepository;
        this.beatRepository      = beatRepository;
        this.purchaseRepository  = purchaseRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public OfferResponse submitOffer(User fan, OfferRequest req) {
        Beat beat = beatRepository.findById(req.getBeatId())
                .orElseThrow(() -> new RuntimeException("Beat não encontrado."));

        if (!beat.isExclusiveNegotiable()) {
            throw new IllegalArgumentException("Este beat não aceita propostas.");
        }
        if (beat.isSoldExclusively()) {
            throw new IllegalArgumentException("Este beat já foi vendido em exclusivo.");
        }
        if (offerRepository.existsByFanIdAndBeatIdAndStatus(fan.getId(), beat.getId(), "PENDING")) {
            throw new IllegalStateException("Já tens uma proposta pendente para este beat.");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor inválido.");
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

        return toDto(offer);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getFanOffers(User fan) {
        return offerRepository.findByFanIdOrderByCreatedAtDesc(fan.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getProducerOffers(User producer) {
        return offerRepository.findByProducerIdOrderByCreatedAtDesc(producer.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public OfferResponse replyToOffer(Long id, User producer, OfferReplyRequest req) {
        ExclusiveOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada."));

        if (!offer.getBeat().getProducer().getId().equals(producer.getId())) {
            throw new SecurityException("Sem permissão.");
        }
        if (!offer.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Esta proposta já foi respondida.");
        }

        String newStatus = req.getStatus() != null ? req.getStatus().toUpperCase() : "";
        if (!newStatus.equals("ACCEPTED") && !newStatus.equals("REJECTED")) {
            throw new IllegalArgumentException("Estado inválido.");
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

            offerRepository.findByBeatIdAndStatus(beat.getId(), "PENDING").forEach(o -> {
                o.setStatus("REJECTED");
                offerRepository.save(o);
                notificationService.notify(o.getFan(), Notification.NotificationType.OFFER_REJECTED,
                        o.getId(), "A tua proposta para \"" + beat.getTitle() + "\" foi recusada.");
            });

            notificationService.notify(offer.getFan(), Notification.NotificationType.OFFER_ACCEPTED,
                    offer.getId(), "A tua proposta para \"" + beat.getTitle() + "\" foi aceite!");
        } else {
            notificationService.notify(offer.getFan(), Notification.NotificationType.OFFER_REJECTED,
                    offer.getId(), "A tua proposta para \"" + beat.getTitle() + "\" foi recusada.");
        }

        return toDto(offer);
    }

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
