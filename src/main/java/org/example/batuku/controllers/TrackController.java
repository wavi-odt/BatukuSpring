package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.CreateTrackRequest;
import org.example.batuku.dto.TrackDetailResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.services.TrackService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class TrackController {

    private final TrackService trackService;
    private final LikeRepository likeRepository;
    private final JwtUserDetailsService jwtUserDetailsService;

    public TrackController(TrackService trackService,
                           LikeRepository likeRepository,
                           JwtUserDetailsService jwtUserDetailsService) {
        this.trackService = trackService;
        this.likeRepository = likeRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    /**
     * Publicação direta de uma faixa: título + género (texto) + ficheiro de
     * áudio + capa opcional, tudo numa só chamada multipart — contrato usado
     * pelo modal Publish.jsx (aba "Faixa").
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> create(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestParam String title,
                                                @RequestParam String genre,
                                                @RequestParam("audio") MultipartFile audio,
                                                @RequestParam(value = "cover", required = false) MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        Track track = trackService.createFromUpload(user, title, genre, audio, cover);
        long likes = likeRepository.countByTrackId(track.getId());
        return ResponseEntity.created(URI.create("/api/tracks/" + track.getId()))
                .body(TrackResponse.from(track, likes));
    }

    @GetMapping
    public List<TrackResponse> listAll() {
        return trackService.listAll().stream()
                .map(t -> TrackResponse.from(t, likeRepository.countByTrackId(t.getId())))
                .toList();
    }

    @GetMapping("/artist/{artistProfileId}")
    public List<TrackResponse> listByArtist(@PathVariable Long artistProfileId) {
        return trackService.listByArtist(artistProfileId).stream()
                .map(t -> TrackResponse.from(t, likeRepository.countByTrackId(t.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public TrackDetailResponse getTrack(@PathVariable Long id) {
        Track t = trackService.findById(id);
        ArtistProfile profile = t.getArtistProfile();

        String genre = null;
        if (t.getGenre() != null) {
            genre = t.getGenre().getName();
        } else if (profile.getGenres() != null && !profile.getGenres().isEmpty()) {
            genre = profile.getGenres().get(0);
        }

        return new TrackDetailResponse(
                t.getId(),
                t.getTitle(),
                profile.getName(),
                profile.getId(),
                t.getCoverUrl(),
                genre,
                SearchController.formatDuration(t.getDurationMs()),
                likeRepository.countByTrackId(t.getId())
        );
    }
}
