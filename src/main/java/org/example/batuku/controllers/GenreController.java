package org.example.batuku.controllers;

import org.example.batuku.domain.Genre;
import org.example.batuku.dto.GenreResponse;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.services.AlbumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreRepository  genreRepository;
    private final TrackRepository  trackRepository;
    private final LikeRepository   likeRepository;
    private final AlbumRepository  albumRepository;
    private final AlbumService     albumService;

    public GenreController(GenreRepository genreRepository,
                           TrackRepository trackRepository,
                           LikeRepository likeRepository,
                           AlbumRepository albumRepository,
                           AlbumService albumService) {
        this.genreRepository = genreRepository;
        this.trackRepository = trackRepository;
        this.likeRepository  = likeRepository;
        this.albumRepository = albumRepository;
        this.albumService    = albumService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<GenreResponse>>> getAll() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : genreRepository.countTracksByGenre()) {
            counts.put((Long) row[0], (Long) row[1]);
        }

        List<GenreResponse> mundiais = genreRepository
                .findAllByCaboverdeanOrderByNameAsc(false)
                .stream().map(g -> toDto(g, counts)).toList();

        List<GenreResponse> caboverde = genreRepository
                .findAllByCaboverdeanOrderByNameAsc(true)
                .stream().map(g -> toDto(g, counts)).toList();

        return ResponseEntity.ok(Map.of("mundiais", mundiais, "caboverde", caboverde));
    }

    @GetMapping("/{id}/tracks")
    public ResponseEntity<List<TrackResponse>> getTracks(@PathVariable Long id) {
        if (!genreRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<TrackResponse> tracks = trackRepository
                .findByGenreIdAndIsPublishedTrueOrderByCreatedAtDesc(id)
                .stream()
                .map(t -> TrackResponse.from(t, likeRepository.countByTrackId(t.getId())))
                .toList();
        return ResponseEntity.ok(tracks);
    }

    @GetMapping("/{id}/releases")
    public ResponseEntity<List<ReleaseResponse>> getReleases(@PathVariable Long id) {
        if (!genreRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<ReleaseResponse> releases = albumRepository.findPublishedByGenreId(id)
                .stream().map(albumService::toResponsePublic).toList();
        return ResponseEntity.ok(releases);
    }

    private GenreResponse toDto(Genre g, Map<Long, Long> counts) {
        return new GenreResponse(g.getId(), g.getName(), g.getHue(), g.isCaboverdean(),
                counts.getOrDefault(g.getId(), 0L));
    }
}
