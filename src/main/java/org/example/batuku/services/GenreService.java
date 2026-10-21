package org.example.batuku.services;

import org.example.batuku.domain.Genre;
import org.example.batuku.dto.GenreResponse;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class GenreService {

    private final GenreRepository genreRepository;
    private final TrackRepository trackRepository;
    private final LikeRepository  likeRepository;
    private final AlbumRepository albumRepository;
    private final AlbumService    albumService;

    public GenreService(GenreRepository genreRepository,
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

    @Transactional(readOnly = true)
    public Map<String, List<GenreResponse>> getAllGenres() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : genreRepository.countTracksByGenre()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        List<GenreResponse> mundiais = genreRepository.findAllByCaboverdeanOrderByNameAsc(false)
                .stream().map(g -> toDto(g, counts)).toList();
        List<GenreResponse> caboverde = genreRepository.findAllByCaboverdeanOrderByNameAsc(true)
                .stream().map(g -> toDto(g, counts)).toList();
        return Map.of("mundiais", mundiais, "caboverde", caboverde);
    }

    @Transactional(readOnly = true)
    public List<TrackResponse> getTracksByGenre(Long id) {
        if (!genreRepository.existsById(id)) throw new NoSuchElementException();
        return trackRepository.findByGenreIdAndIsPublishedTrueOrderByCreatedAtDesc(id)
                .stream()
                .map(t -> TrackResponse.from(t, likeRepository.countByTrackId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReleaseResponse> getReleasesByGenre(Long id) {
        if (!genreRepository.existsById(id)) throw new NoSuchElementException();
        return albumRepository.findPublishedByGenreId(id)
                .stream().map(albumService::toResponsePublic).toList();
    }

    private GenreResponse toDto(Genre g, Map<Long, Long> counts) {
        return new GenreResponse(g.getId(), g.getName(), g.getHue(), g.isCaboverdean(),
                counts.getOrDefault(g.getId(), 0L));
    }
}
