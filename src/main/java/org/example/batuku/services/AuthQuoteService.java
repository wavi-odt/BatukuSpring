package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.AuthQuoteConfig;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.AuthQuoteConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthQuoteService {

    public record AuthQuoteResponse(String quote, String artistName, String genre, String location) {}
    public record AuthQuoteAdminResponse(String quote, Long artistProfileId, String artistName, String genre, String location) {}

    private final AuthQuoteConfigRepository repository;
    private final ArtistProfileRepository artistProfileRepository;

    public AuthQuoteService(AuthQuoteConfigRepository repository,
                            ArtistProfileRepository artistProfileRepository) {
        this.repository = repository;
        this.artistProfileRepository = artistProfileRepository;
    }

    public AuthQuoteResponse getPublic(String page) {
        return repository.findById(page.toUpperCase())
                .filter(c -> c.getQuoteText() != null && !c.getQuoteText().isBlank())
                .map(c -> {
                    ArtistProfile p = c.getArtistProfile();
                    if (p == null) return new AuthQuoteResponse(c.getQuoteText(), null, null, null);
                    return new AuthQuoteResponse(c.getQuoteText(), p.getName(), firstGenre(p.getGenres()), p.getLocation());
                })
                .orElse(null);
    }

    public AuthQuoteAdminResponse getAdmin(String page) {
        return repository.findById(page.toUpperCase())
                .map(c -> {
                    ArtistProfile p = c.getArtistProfile();
                    return new AuthQuoteAdminResponse(
                            c.getQuoteText(),
                            p != null ? p.getId() : null,
                            p != null ? p.getName() : null,
                            p != null ? firstGenre(p.getGenres()) : null,
                            p != null ? p.getLocation() : null
                    );
                })
                .orElse(new AuthQuoteAdminResponse(null, null, null, null, null));
    }

    @Transactional
    public void save(String page, String quoteText, Long artistProfileId) {
        AuthQuoteConfig config = repository.findById(page.toUpperCase()).orElseGet(AuthQuoteConfig::new);
        config.setPage(page.toUpperCase());
        config.setQuoteText(quoteText);
        config.setArtistProfile(
            artistProfileId != null
                ? artistProfileRepository.findById(artistProfileId).orElse(null)
                : null
        );
        repository.save(config);
    }

    @Transactional
    public void clear(String page) {
        repository.deleteById(page.toUpperCase());
    }

    private String firstGenre(List<String> genres) {
        return (genres != null && !genres.isEmpty()) ? genres.get(0) : null;
    }
}
