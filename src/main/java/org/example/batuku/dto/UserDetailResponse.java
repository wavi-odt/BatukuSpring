package org.example.batuku.dto;

public record UserDetailResponse(
        Long id,
        String name,
        String handle,
        String imageUrl,
        String bio,
        long followers,
        long following,
        boolean isFollowing,
        int level
) {}
