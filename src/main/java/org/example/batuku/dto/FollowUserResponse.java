package org.example.batuku.dto;

import org.example.batuku.domain.User;

public class FollowUserResponse {

    private Long id;
    private String username;
    private String name;
    private String avatarUrl;

    public static FollowUserResponse from(User user) {
        FollowUserResponse r = new FollowUserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.name = user.getName();
        r.avatarUrl = user.getAvatarUrl();
        return r;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
}
