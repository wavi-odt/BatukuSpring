package org.example.batuku.oauth2;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google"  -> new GoogleOAuth2UserInfo(attributes);
            case "discord" -> new DiscordOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("Provedor OAuth2 não suportado: " + registrationId);
        };
    }
}
