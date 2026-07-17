package org.example.batuku.services;

import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.oauth2.OAuth2UserInfo;
import org.example.batuku.oauth2.OAuth2UserInfoFactory;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_found"),
                "Email não disponível no perfil " + registrationId + ". Garante que o email está verificado."
            );
        }

        findOrCreateUser(registrationId, userInfo);
        return oAuth2User;
    }

    private User findOrCreateUser(String provider, OAuth2UserInfo userInfo) {
        // 1. Conta OAuth2 já existente para este provedor + ID externo
        Optional<User> byProvider = userRepository.findByProviderAndProviderId(provider, userInfo.getId());
        if (byProvider.isPresent()) {
            User user = byProvider.get();
            user.setAvatarUrl(userInfo.getAvatarUrl());
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        // 2. Email já existe com conta local — liga o provedor OAuth2
        Optional<User> byEmail = userRepository.findByEmail(userInfo.getEmail().trim().toLowerCase());
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setProvider(provider);
            user.setProviderId(userInfo.getId());
            if (userInfo.getAvatarUrl() != null) {
                user.setAvatarUrl(userInfo.getAvatarUrl());
            }
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        // 3. Utilizador novo — criar conta automaticamente
        return createUser(provider, userInfo);
    }

    private User createUser(String provider, OAuth2UserInfo userInfo) {
        Role fanRole = roleRepository.findByName("ROLE_FAN")
                .orElseThrow(() -> new RuntimeException("ROLE_FAN não encontrada. Verifica o SeedRoles."));

        User user = new User();
        user.setEmail(userInfo.getEmail().trim().toLowerCase());
        user.setUsername(generateUsername(userInfo));
        user.setName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail().split("@")[0]);
        user.setAvatarUrl(userInfo.getAvatarUrl());
        user.setPassword(UUID.randomUUID().toString());
        user.setProvider(provider);
        user.setProviderId(userInfo.getId());
        user.setUserRole(User.UserRole.FAN);
        user.setRoles(Set.of(fanRole));
        user.setEnabled(true);

        return userRepository.save(user);
    }

    private String generateUsername(OAuth2UserInfo userInfo) {
        String base = userInfo.getEmail().split("@")[0]
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "");

        if (base.length() < 3) base = base + "user";
        if (base.length() > 40) base = base.substring(0, 40);

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
