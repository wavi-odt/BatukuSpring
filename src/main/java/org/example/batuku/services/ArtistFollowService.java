package org.example.batuku.services;

import org.example.batuku.domain.ArtistFollow;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public ArtistFollowService(ArtistFollowRepository artistFollowRepository,
                               ArtistProfileRepository artistProfileRepository) {
        this.artistFollowRepository = artistFollowRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional
    public long follow(User follower, Long artistProfileId) {
        if (artistFollowRepository.existsByFollowerIdAndArtistProfileId(follower.getId(), artistProfileId)) {
            throw new RuntimeException("Já segues este artista.");
        }
        ArtistProfile profile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artista não encontrado."));
        ArtistFollow follow = new ArtistFollow();
        follow.setFollower(follower);
        follow.setArtistProfile(profile);
        artistFollowRepository.save(follow);
        return artistFollowRepository.countByArtistProfileId(artistProfileId);
    }

    @Transactional
    public long unfollow(User follower, Long artistProfileId) {
        artistFollowRepository.deleteByFollowerIdAndArtistProfileId(follower.getId(), artistProfileId);
        return artistFollowRepository.countByArtistProfileId(artistProfileId);
    }

    public boolean isFollowing(Long followerId, Long artistProfileId) {
        return artistFollowRepository.existsByFollowerIdAndArtistProfileId(followerId, artistProfileId);
    }

    public long followerCount(Long artistProfileId) {
        return artistFollowRepository.countByArtistProfileId(artistProfileId);
    }
}
