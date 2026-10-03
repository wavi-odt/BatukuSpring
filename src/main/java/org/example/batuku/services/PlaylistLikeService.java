package org.example.batuku.services;

import org.example.batuku.domain.Playlist;
import org.example.batuku.domain.PlaylistLike;
import org.example.batuku.domain.User;
import org.example.batuku.repository.PlaylistLikeRepository;
import org.example.batuku.repository.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaylistLikeService {

    private final PlaylistLikeRepository playlistLikeRepository;
    private final PlaylistRepository     playlistRepository;
    private final GamificationService    gamificationService;

    public PlaylistLikeService(PlaylistLikeRepository playlistLikeRepository,
                               PlaylistRepository playlistRepository,
                               GamificationService gamificationService) {
        this.playlistLikeRepository = playlistLikeRepository;
        this.playlistRepository     = playlistRepository;
        this.gamificationService    = gamificationService;
    }

    @Transactional
    public void like(User user, Long playlistId) {
        if (playlistLikeRepository.existsByUserIdAndPlaylistId(user.getId(), playlistId)) return;

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada."));

        // Utilizador não pode dar like na própria playlist
        if (playlist.getUser() != null && playlist.getUser().getId().equals(user.getId())) return;

        PlaylistLike like = new PlaylistLike();
        like.setUser(user);
        like.setPlaylist(playlist);
        playlistLikeRepository.save(like);

        // Verificar badges do dono da playlist (Curador / Influenciador)
        if (playlist.getUser() != null) {
            gamificationService.checkBadgesForUser(playlist.getUser());
        }
    }

    @Transactional
    public void unlike(User user, Long playlistId) {
        playlistLikeRepository.deleteByUserIdAndPlaylistId(user.getId(), playlistId);
    }

    public record LikeStatus(boolean liked, long count) {}

    public LikeStatus status(User user, Long playlistId) {
        boolean liked = playlistLikeRepository.existsByUserIdAndPlaylistId(user.getId(), playlistId);
        long count    = playlistLikeRepository.countByPlaylistId(playlistId);
        return new LikeStatus(liked, count);
    }
}
