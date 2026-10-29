package org.example.batuku.oauth2;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getAvatarUrl() {
        String picture = (String) attributes.get("picture");
        if (picture == null) return null;
        // Google returns ~96px by default (=s96-c); request a larger version
        return picture.replaceAll("=s\\d+-c$", "=s400-c");
    }
}
