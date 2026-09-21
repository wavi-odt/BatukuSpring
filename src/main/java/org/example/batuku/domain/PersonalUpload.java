package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_uploads")
public class PersonalUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "share_token", nullable = false, unique = true, length = 36)
    private String shareToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PersonalUpload() {}

    public PersonalUpload(User user, String title, String audioUrl, String shareToken) {
        this.user = user;
        this.title = title;
        this.audioUrl = audioUrl;
        this.shareToken = shareToken;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getShareToken() { return shareToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
}
