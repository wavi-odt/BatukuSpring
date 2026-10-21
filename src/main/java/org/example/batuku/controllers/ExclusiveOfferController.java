package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.OfferReplyRequest;
import org.example.batuku.dto.OfferRequest;
import org.example.batuku.dto.OfferResponse;
import org.example.batuku.services.ExclusiveOfferService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace/offers")
public class ExclusiveOfferController {

    private final ExclusiveOfferService  exclusiveOfferService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public ExclusiveOfferController(ExclusiveOfferService exclusiveOfferService,
                                    JwtUserDetailsService jwtUserDetailsService) {
        this.exclusiveOfferService = exclusiveOfferService;
        this.jwtUserDetailsService  = jwtUserDetailsService;
    }

    @PostMapping
    public ResponseEntity<?> submeterOferta(@RequestBody OfferRequest req,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User fan = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(exclusiveOfferService.submitOffer(fan, req));
    }

    @GetMapping("/mine")
    public List<OfferResponse> minhasOfertas(@AuthenticationPrincipal UserDetails userDetails) {
        User fan = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return exclusiveOfferService.getFanOffers(fan);
    }

    @GetMapping("/received")
    public List<OfferResponse> ofertasRecebidas(@AuthenticationPrincipal UserDetails userDetails) {
        User producer = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return exclusiveOfferService.getProducerOffers(producer);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> responderOferta(@PathVariable Long id,
                                             @RequestBody OfferReplyRequest req,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User producer = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            return ResponseEntity.ok(exclusiveOfferService.replyToOffer(id, producer, req));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
