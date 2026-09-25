package org.example.batuku.services;

import org.example.batuku.config.TierProperties;
import org.example.batuku.domain.ArtistFollow;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Notification;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistFollowResponse;
import org.example.batuku.dto.FanResponse;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.FollowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final FollowRepository followRepository;
    private final TierProperties tierProperties;
    private final NotificationService notificationService;

    public ArtistFollowService(ArtistFollowRepository artistFollowRepository,
                               ArtistProfileRepository artistProfileRepository,
                               FollowRepository followRepository,
                               TierProperties tierProperties,
                               NotificationService notificationService) {
        this.artistFollowRepository = artistFollowRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.followRepository = followRepository;
        this.tierProperties = tierProperties;
        this.notificationService = notificationService;
    }

    @Transactional
    public long follow(User follower, Long artistProfileId) {
        ArtistProfile profile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artista não encontrado."));

        if (profile.getUser() != null && profile.getUser().getId().equals(follower.getId())) {
            throw new RuntimeException("Não podes seguir o teu próprio perfil.");
        }
        if (artistFollowRepository.existsByFollowerIdAndArtistProfileId(follower.getId(), artistProfileId)) {
            throw new RuntimeException("Já segues este artista.");
        }

        // migrar follow stale: se o artista tem conta e o follower já o seguia via `follows`, remover
        if (profile.getUser() != null) {
            followRepository.deleteByFollowerIdAndFolloweeId(follower.getId(), profile.getUser().getId());
        }

        ArtistFollow follow = new ArtistFollow();
        follow.setFollower(follower);
        follow.setArtistProfile(profile);
        artistFollowRepository.save(follow);

        User artistUser = profile.getUser();
        if (artistUser != null && !artistUser.getId().equals(follower.getId())) {
            String followerName = (follower.getName() != null && !follower.getName().isBlank()) ? follower.getName() : follower.getUsername();
            notificationService.notify(artistUser, Notification.NotificationType.FOLLOW, follower.getId(),
                    followerName + " começou a seguir-te");
        }

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

    public List<FanResponse> listFans(User artist) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artist.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de artista não encontrado."));
        return artistFollowRepository.findFansWithStatsByArtistProfile(profile.getId())
                .stream()
                .map(p -> FanResponse.from(p, tierProperties))
                .toList();
    }

    public List<ArtistFollowResponse> listFollowed(User user) {
        return listFollowedByUserId(user.getId());
    }

    public List<ArtistFollowResponse> listFollowedByUserId(Long userId) {
        Set<Long> seen = new HashSet<>();
        List<ArtistFollowResponse> result = new ArrayList<>();

        for (ArtistFollow f : artistFollowRepository.findByFollowerIdOrderByCreatedAtDesc(userId)) {
            ArtistProfile p = f.getArtistProfile();
            if (seen.add(p.getId())) {
                result.add(ArtistFollowResponse.from(p, artistFollowRepository.countByArtistProfileId(p.getId()), 0L));
            }
        }

        for (ArtistProfile p : artistProfileRepository.findByFollowerViaUserFollow(userId)) {
            if (seen.add(p.getId())) {
                result.add(ArtistFollowResponse.from(p, artistFollowRepository.countByArtistProfileId(p.getId()), 0L));
            }
        }

        return result;
    }
}
