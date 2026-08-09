package org.example.batuku.dto;

import org.example.batuku.config.TierProperties;

import java.time.LocalDateTime;

public record FanResponse(
        Long id,
        String name,
        String handle,
        String avatarUrl,
        String location,
        LocalDateTime followedAt,
        LocalDateTime lastPlayedAt,
        long plays,
        long likes,
        long comments,
        String tier
) {
    public static FanResponse from(FanProjection p, TierProperties tier) {
        long plays    = p.getPlays()    != null ? p.getPlays()    : 0L;
        long likes    = p.getLikes()    != null ? p.getLikes()    : 0L;
        long comments = p.getComments() != null ? p.getComments() : 0L;
        return new FanResponse(
                p.getId(),
                p.getName(),
                "@" + p.getUsername(),
                p.getAvatarUrl(),
                p.getLocation(),
                p.getFollowedAt(),
                p.getLastPlayedAt(),
                plays,
                likes,
                comments,
                tier.computeTier(plays, likes, comments)
        );
    }
}
