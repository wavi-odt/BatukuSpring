package org.example.batuku.services;

import org.example.batuku.domain.Like;
import org.example.batuku.domain.Playlist;
import org.example.batuku.domain.PlaylistTrack;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlaylistRepository;
import org.example.batuku.repository.PlaylistTrackRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final TrackRepository trackRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;

    public LikeService(LikeRepository likeRepository,
                       TrackRepository trackRepository,
                       PlaylistRepository playlistRepository,
                       PlaylistTrackRepository playlistTrackRepository) {
        this.likeRepository = likeRepository;
        this.trackRepository = trackRepository;
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    @Transactional
    public void like(User user, Long trackId) {
        if (likeRepository.existsByUserIdAndTrackId(user.getId(), trackId)) return; // idempotente

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Faixa não encontrada."));

        Like like = new Like();
        like.setUser(user);
        like.setTrack(track);
        likeRepository.save(like);

        // Adicionar aos Favoritos
        Playlist favorites = getOrCreateFavorites(user);
        if (!playlistTrackRepository.existsByPlaylistIdAndTrackId(favorites.getId(), trackId)) {
            int pos = (int) playlistTrackRepository.countByPlaylistId(favorites.getId()) + 1;
            PlaylistTrack pt = new PlaylistTrack();
            pt.setPlaylist(favorites);
            pt.setTrack(track);
            pt.setPosition(pos);
            playlistTrackRepository.save(pt);
        }
    }

    @Transactional
    public void unlike(User user, Long trackId) {
        likeRepository.deleteByUserIdAndTrackId(user.getId(), trackId);

        // Remover dos Favoritos
        playlistRepository.findByUserIdAndIsSystemGeneratedTrue(user.getId())
                .ifPresent(fav -> playlistTrackRepository.deleteByPlaylistIdAndTrackId(fav.getId(), trackId));
    }

    public record LikeStatus(boolean liked, long count) {}

    public LikeStatus status(User user, Long trackId) {
        boolean liked = likeRepository.existsByUserIdAndTrackId(user.getId(), trackId);
        long count    = likeRepository.countByTrackId(trackId);
        return new LikeStatus(liked, count);
    }

    private Playlist getOrCreateFavorites(User user) {
        return playlistRepository.findByUserIdAndIsSystemGeneratedTrue(user.getId())
                .orElseGet(() -> {
                    Playlist fav = new Playlist();
                    fav.setUser(user);
                    fav.setTitle("Favoritos");
                    fav.setSystemGenerated(true);
                    return playlistRepository.save(fav);
                });
    }
}
