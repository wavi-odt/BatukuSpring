package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.BeatEditRequest;
import org.example.batuku.dto.BeatResponse;
import org.example.batuku.dto.ProducerResponse;
import org.example.batuku.dto.PurchaseRequest;
import org.example.batuku.dto.PurchaseResponse;
import org.example.batuku.services.MarketplaceService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MarketplaceService     marketplaceService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public MarketplaceController(MarketplaceService marketplaceService,
                                 JwtUserDetailsService jwtUserDetailsService) {
        this.marketplaceService    = marketplaceService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping("/beats")
    public List<BeatResponse> listarBeats(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String sort) {
        return marketplaceService.listarBeats(genre, sort);
    }

    @GetMapping("/beats/featured")
    public ResponseEntity<BeatResponse> obterDestaque() {
        return marketplaceService.obterDestaque()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/genres")
    public List<String> listarGeneros() {
        return marketplaceService.listarGeneros();
    }

    @GetMapping("/stats")
    public Map<String, Object> obterStats() {
        return marketplaceService.obterStats();
    }

    @GetMapping("/producers")
    public List<ProducerResponse> obterTopProdutores(
            @RequestParam(defaultValue = "6") int limit) {
        return marketplaceService.obterTopProdutores(limit);
    }

    @PostMapping(value = "/beats", consumes = "multipart/form-data")
    public ResponseEntity<BeatResponse> publicarBeat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer bpm,
            @RequestParam(value = "key", required = false) String musicalKey,
            @RequestParam(required = false) Integer hue,
            @RequestParam(required = false) BigDecimal leasePrice,
            @RequestParam(required = false) BigDecimal premiumPrice,
            @RequestParam(required = false) BigDecimal exclusivePrice,
            @RequestParam(required = false, defaultValue = "false") boolean isNew,
            @RequestParam(required = false, defaultValue = "false") boolean isFeatured,
            @RequestParam(required = false, defaultValue = "false") boolean exclusiveNegotiable,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        BeatResponse beat = marketplaceService.createBeat(
                user.getId(), title, genre, bpm, musicalKey, hue,
                leasePrice, premiumPrice, exclusivePrice, isNew, isFeatured, exclusiveNegotiable, audio, cover);
        return ResponseEntity.status(HttpStatus.CREATED).body(beat);
    }

    @GetMapping("/my-beats")
    public List<BeatResponse> meusBeats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return marketplaceService.listarMeusBeats(user.getId());
    }

    @GetMapping("/my-stats")
    public Map<String, Object> meusStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return marketplaceService.obterMeusStats(user.getId());
    }

    @PatchMapping("/beats/{id}")
    public ResponseEntity<BeatResponse> editarBeat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody BeatEditRequest req) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(marketplaceService.editarBeat(user.getId(), id, req));
    }

    @DeleteMapping("/beats/{id}")
    public ResponseEntity<Void> eliminarBeat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        marketplaceService.eliminarBeat(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-purchases")
    public List<PurchaseResponse> minhasCompras(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return marketplaceService.listarMinhasCompras(user.getId());
    }

    @PostMapping("/purchase")
    public ResponseEntity<Void> comprar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PurchaseRequest req) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        marketplaceService.comprar(user.getId(), req);
        return ResponseEntity.ok().build();
    }
}
