package org.example.batuku.oauth2;

import java.util.Map;

public class DiscordOAuth2UserInfo extends OAuth2UserInfo {

    public DiscordOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getName() {
        String globalName = (String) attributes.get("global_name");
        return globalName != null ? globalName : (String) attributes.get("username");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    public String getDiscordUsername() {
        return (String) attributes.get("username");
    }

    @Override
    public String getAvatarUrl() {
        String id = getId();
        String avatar = (String) attributes.get("avatar");
        if (id == null || avatar == null) return null;
        return "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png?size=256";
    }
}
