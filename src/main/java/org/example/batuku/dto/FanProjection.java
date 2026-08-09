package org.example.batuku.dto;

import java.time.LocalDateTime;

public interface FanProjection {
    Long getId();
    String getName();
    String getUsername();
    String getAvatarUrl();
    String getLocation();
    LocalDateTime getFollowedAt();
    LocalDateTime getLastPlayedAt();
    Long getPlays();
    Long getLikes();
    Long getComments();
}
