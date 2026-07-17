package org.example.batuku.services;

import org.example.batuku.domain.Like;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final TrackRepository trackRepository;

    public LikeService(LikeRepository likeRepository, TrackRepository trackRepository) {
        this.likeRepository = likeRepository;
        this.trackRepository = trackRepository;
    }

    @Transactional
    public void like(User user, Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Faixa não encontrada."));
        if (likeRepository.existsByUserIdAndTrackId(user.getId(), trackId)) {
            throw new RuntimeException("Já deste like a esta faixa.");
        }
        Like like = new Like();
        like.setUser(user);
        like.setTrack(track);
        likeRepository.save(like);
    }

    @Transactional
    public void unlike(User user, Long trackId) {
        likeRepository.deleteByUserIdAndTrackId(user.getId(), trackId);
    }
}
