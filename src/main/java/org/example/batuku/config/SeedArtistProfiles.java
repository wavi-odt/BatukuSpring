package org.example.batuku.config;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class SeedArtistProfiles {

    @Bean
    @Order(3)
    CommandLineRunner seedArtistProfilesRunner(UserRepository userRepository,
                                               ArtistProfileRepository artistProfileRepository) {
        return args -> {
            List<User> artists = userRepository.findAll().stream()
                    .filter(u -> u.getUserRole() == User.UserRole.ARTIST)
                    .filter(u -> artistProfileRepository.findByUserId(u.getId()).isEmpty())
                    .toList();

            for (User u : artists) {
                ArtistProfile profile = new ArtistProfile();
                profile.setName(u.getName());
                profile.setImageUrl(u.getAvatarUrl());
                profile.setSpotifyArtistId(null);
                profile.setSpotifyUrl(null);
                profile.setClaimed(true);
                profile.setUser(u);
                artistProfileRepository.save(profile);
                System.out.println("ArtistProfile criado para: " + u.getUsername());
            }

            if (!artists.isEmpty()) {
                System.out.println(artists.size() + " ArtistProfile(s) criado(s).");
            }
        };
    }
}
