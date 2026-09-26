package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.services.AlbumService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/releases")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ReleaseController {

    private final AlbumService albumService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public ReleaseController(AlbumService albumService, JwtUserDetailsService jwtUserDetailsService) {
        this.albumService = albumService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    /** Passo 1: cria o álbum em modo DRAFT (metadata + capa, sem faixas). */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ReleaseResponse> createDraft(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam String genre,
            @RequestParam String releaseType,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        ReleaseResponse response = albumService.createDraft(user, title, genre, releaseType, cover);
        return ResponseEntity.created(URI.create("/api/releases/" + response.getId())).body(response);
    }

    /** Passo 2: adiciona uma faixa ao álbum DRAFT. Chamado uma vez por faixa. */
    @PostMapping(value = "/{id}/tracks", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> addTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam("audio") MultipartFile audio) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        TrackResponse track = albumService.addTrack(id, user, title, genre, audio);
        return ResponseEntity.ok(track);
    }

    /** Passo 3: publica o álbum (DRAFT para PUBLISHED). */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ReleaseResponse> publish(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(albumService.publish(id, user));
    }

    /** Actualiza o título do lançamento. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ReleaseResponse> updateTitle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(albumService.updateTitle(id, user, body.get("title")));
    }

    /** Substitui a capa do lançamento. */
    @PatchMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ReleaseResponse> updateCover(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("cover") MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(albumService.updateCover(id, user, cover));
    }

    /** Elimina um lançamento publicado. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<Void> deleteRelease(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        albumService.deleteRelease(id, user);
        return ResponseEntity.noContent().build();
    }

    /** Limpeza: apaga o DRAFT se o upload falhou a meio. */
    @DeleteMapping("/{id}/draft")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        albumService.deleteDraft(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ReleaseResponse getRelease(@PathVariable Long id) {
        return albumService.findById(id);
    }

    @GetMapping("/artist/{artistProfileId}")
    public List<ReleaseResponse> listByArtist(@PathVariable Long artistProfileId) {
        return albumService.listByArtist(artistProfileId);
    }

    /** Todos os lançamentos do artista autenticado (PUBLISHED + DRAFT). */
    @GetMapping("/my")
    @PreAuthorize("hasRole('ARTIST')")
    public List<ReleaseResponse> listMine(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return albumService.listAllMine(user);
    }
}
