package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.CreateTrackRequest;
import org.example.batuku.dto.TrackDetailResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.AlbumTrackRepository;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.services.TrackService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class TrackController {

    private final TrackService trackService;
    private final TrackRepository trackRepository;
    private final LikeRepository likeRepository;
    private final PlayRepository playRepository;
    private final CommentRepository commentRepository;
    private final AlbumTrackRepository albumTrackRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final JwtUserDetailsService jwtUserDetailsService;

    public TrackController(TrackService trackService,
                           TrackRepository trackRepository,
                           LikeRepository likeRepository,
                           PlayRepository playRepository,
                           CommentRepository commentRepository,
                           AlbumTrackRepository albumTrackRepository,
                           ArtistFollowRepository artistFollowRepository,
                           JwtUserDetailsService jwtUserDetailsService) {
        this.trackService = trackService;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.playRepository = playRepository;
        this.commentRepository = commentRepository;
        this.albumTrackRepository = albumTrackRepository;
        this.artistFollowRepository = artistFollowRepository;
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
                                                @RequestParam(value = "cover", required = false) MultipartFile cover,
                                                @RequestParam(value = "scheduledAt", required = false) String scheduledAt) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        LocalDateTime scheduled = (scheduledAt != null && !scheduledAt.isBlank())
                ? LocalDateTime.parse(scheduledAt) : null;
        Track track = trackService.createFromUpload(user, title, genre, audio, cover, scheduled);
        long likes = likeRepository.countByTrackId(track.getId());
        return ResponseEntity.created(URI.create("/api/tracks/" + track.getId()))
                .body(TrackResponse.from(track, likes));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> update(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            Track track = trackService.update(id, body.get("title"), body.get("genre"), user);
            return ResponseEntity.ok(TrackResponse.from(track,
                    likeRepository.countByTrackId(track.getId()),
                    playRepository.countByTrackId(track.getId()),
                    commentRepository.countByTrackId(track.getId())));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            trackService.delete(id, user);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/admin/backfill-durations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> backfillDurations() {
        return ResponseEntity.ok(trackService.backfillDurations());
    }

    @GetMapping
    public List<TrackResponse> listAll() {
        return trackService.listAll().stream()
                .map(t -> TrackResponse.from(t, likeRepository.countByTrackId(t.getId())))
                .toList();
    }

    @PatchMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> updateCover(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long id,
                                                     @RequestParam("cover") MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            Track track = trackService.updateCover(id, user, cover);
            TrackResponse r = TrackResponse.from(track,
                    likeRepository.countByTrackId(track.getId()),
                    playRepository.countByTrackId(track.getId()),
                    commentRepository.countByTrackId(track.getId()));
            r.setBelongsToRelease(false);
            return ResponseEntity.ok(r);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** GET /api/tracks/following — faixas recentes publicadas pelos artistas que o utilizador segue. */
    @GetMapping("/following")
    public ResponseEntity<List<TrackResponse>> getFollowingTracks(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        List<Long> artistIds = artistFollowRepository.findByFollowerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(f -> f.getArtistProfile().getId()).toList();
        if (artistIds.isEmpty()) return ResponseEntity.ok(List.of());
        List<Track> tracks = trackRepository.findTop20ByArtistProfileIdInAndIsPublishedTrueOrderByCreatedAtDesc(artistIds);
        return ResponseEntity.ok(tracks.stream()
                .map(t -> TrackResponse.from(t,
                        likeRepository.countByTrackId(t.getId()),
                        playRepository.countByTrackId(t.getId()),
                        0L))
                .toList());
    }

    @GetMapping("/artist/{artistProfileId}")
    public List<TrackResponse> listByArtist(@PathVariable Long artistProfileId) {
        return trackService.listByArtist(artistProfileId).stream()
                .map(t -> {
                    TrackResponse r = TrackResponse.from(t,
                            likeRepository.countByTrackId(t.getId()),
                            playRepository.countByTrackId(t.getId()),
                            commentRepository.countByTrackId(t.getId()));
                    r.setBelongsToRelease(albumTrackRepository.existsByTrackId(t.getId()));
                    return r;
                })
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
