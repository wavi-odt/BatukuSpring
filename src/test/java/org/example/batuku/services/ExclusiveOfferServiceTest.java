package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.Beat;
import org.example.batuku.domain.ExclusiveOffer;
import org.example.batuku.domain.User;
import org.example.batuku.dto.OfferReplyRequest;
import org.example.batuku.dto.OfferRequest;
import org.example.batuku.dto.OfferResponse;
import org.example.batuku.repository.BeatRepository;
import org.example.batuku.repository.ExclusiveOfferRepository;
import org.example.batuku.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExclusiveOfferServiceTest {

    @Mock ExclusiveOfferRepository offerRepository;
    @Mock BeatRepository           beatRepository;
    @Mock PurchaseRepository       purchaseRepository;
    @Mock NotificationService      notificationService;
    @InjectMocks ExclusiveOfferService service;

    private User fan;
    private User producer;
    private Beat beat;

    @BeforeEach
    void setUp() {
        fan      = TestData.user(1L, User.UserRole.FAN);
        producer = TestData.user(2L, User.UserRole.FAN);
        beat     = TestData.beat(10L, producer);
        beat.setExclusiveNegotiable(true);
    }

    private OfferRequest req(BigDecimal amount) {
        OfferRequest r = new OfferRequest();
        r.setBeatId(10L);
        r.setAmount(amount);
        return r;
    }

    @Test
    @DisplayName("submitOffer lança RuntimeException quando beat não existe")
    void submitOfferBeatNaoExiste() {
        when(beatRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.submitOffer(fan, req(new BigDecimal("50"))));
    }

    @Test
    @DisplayName("submitOffer lança IllegalArgumentException quando beat não aceita propostas")
    void submitOfferNaoNegociavel() {
        beat.setExclusiveNegotiable(false);
        when(beatRepository.findById(10L)).thenReturn(Optional.of(beat));
        assertThrows(IllegalArgumentException.class, () -> service.submitOffer(fan, req(new BigDecimal("50"))));
    }

    @Test
    @DisplayName("submitOffer lança IllegalArgumentException quando beat já foi vendido")
    void submitOfferJaVendido() {
        beat.setSoldExclusively(true);
        when(beatRepository.findById(10L)).thenReturn(Optional.of(beat));
        assertThrows(IllegalArgumentException.class, () -> service.submitOffer(fan, req(new BigDecimal("50"))));
    }

    @Test
    @DisplayName("submitOffer lança IllegalStateException quando já existe proposta pendente")
    void submitOfferPendente() {
        when(beatRepository.findById(10L)).thenReturn(Optional.of(beat));
        when(offerRepository.existsByFanIdAndBeatIdAndStatus(1L, 10L, "PENDING")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.submitOffer(fan, req(new BigDecimal("50"))));
    }

    @Test
    @DisplayName("submitOffer lança IllegalArgumentException quando valor é inválido")
    void submitOfferValorInvalido() {
        when(beatRepository.findById(10L)).thenReturn(Optional.of(beat));
        when(offerRepository.existsByFanIdAndBeatIdAndStatus(1L, 10L, "PENDING")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.submitOffer(fan, req(BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("submitOffer guarda proposta e devolve OfferResponse quando válida")
    void submitOfferValida() {
        when(beatRepository.findById(10L)).thenReturn(Optional.of(beat));
        when(offerRepository.existsByFanIdAndBeatIdAndStatus(1L, 10L, "PENDING")).thenReturn(false);
        when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse r = service.submitOffer(fan, req(new BigDecimal("50")));

        assertNotNull(r);
        verify(offerRepository).save(any());
        verify(notificationService).notify(any(), any(), any(), any());
    }

    @Test
    @DisplayName("replyToOffer lança SecurityException quando produtor não é dono do beat")
    void replyToOfferSemPermissao() {
        User outro = TestData.user(99L, User.UserRole.FAN);
        ExclusiveOffer offer = new ExclusiveOffer();
        offer.setFan(fan);
        offer.setBeat(beat);
        when(offerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(offer));

        OfferReplyRequest req = new OfferReplyRequest();
        req.setStatus("ACCEPTED");

        assertThrows(SecurityException.class, () -> service.replyToOffer(1L, outro, req));
    }

    @Test
    @DisplayName("replyToOffer lança IllegalArgumentException quando proposta já foi respondida")
    void replyToOfferJaRespondida() {
        ExclusiveOffer offer = new ExclusiveOffer();
        offer.setFan(fan);
        offer.setBeat(beat);
        offer.setStatus("ACCEPTED");
        when(offerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(offer));

        OfferReplyRequest req = new OfferReplyRequest();
        req.setStatus("REJECTED");

        assertThrows(IllegalArgumentException.class, () -> service.replyToOffer(1L, producer, req));
    }

    @Test
    @DisplayName("replyToOffer lança IllegalArgumentException para estado inválido")
    void replyToOfferEstadoInvalido() {
        ExclusiveOffer offer = new ExclusiveOffer();
        offer.setFan(fan);
        offer.setBeat(beat);
        when(offerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(offer));

        OfferReplyRequest req = new OfferReplyRequest();
        req.setStatus("UNKNOWN");

        assertThrows(IllegalArgumentException.class, () -> service.replyToOffer(1L, producer, req));
    }

    @Test
    @DisplayName("getFanOffers devolve lista de propostas do fan")
    void getFanOffers() {
        when(offerRepository.findByFanIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<OfferResponse> result = service.getFanOffers(fan);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getProducerOffers devolve lista de propostas recebidas pelo produtor")
    void getProducerOffers() {
        when(offerRepository.findByProducerIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());

        List<OfferResponse> result = service.getProducerOffers(producer);

        assertNotNull(result);
    }
}
