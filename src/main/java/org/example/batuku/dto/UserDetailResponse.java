package org.example.batuku.dto;

import java.util.List;

public record UserDetailResponse(
        Long id,
        String name,
        String handle,
        String imageUrl,
        String bio,
        String location,
        long followers,
        long following,
        boolean isFollowing,
        int level,
        Long artistProfileId,
        int points,
        long rank,
        int badgesCount,
        List<BadgeResult> badges
) {
    public record BadgeResult(
            Long id,
            String name,
            String description,
            String iconUrl,
            int pointsRequired,
            boolean got,
            String earnedAt
    ) {}
}
