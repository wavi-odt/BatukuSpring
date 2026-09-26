package org.example.batuku.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "auth_quote_config")
public class AuthQuoteConfig {

    /** "LOGIN" ou "REGISTER", chave natural da linha. */
    @Id
    @Column(name = "page", length = 20)
    private String page;

    @Column(name = "quote_text", columnDefinition = "TEXT")
    private String quoteText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id")
    private ArtistProfile artistProfile;

    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }

    public String getQuoteText() { return quoteText; }
    public void setQuoteText(String quoteText) { this.quoteText = quoteText; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }
}
