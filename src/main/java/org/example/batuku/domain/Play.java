package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plays")
public class Play {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PlaySource source;

    @Column(length = 100)
    private String country;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "duration_played")
    private Integer durationPlayed;

    @Column(name = "is_full_play", nullable = false)
    private boolean isFullPlay = false;

    @Column(name = "referrer_url", length = 500)
    private String referrerUrl;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    public enum PlaySource { WEB, DISCORD, SHARE_LINK }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

    public LocalDateTime getPlayedAt() { return playedAt; }

    public PlaySource getSource() { return source; }
    public void setSource(PlaySource source) { this.source = source; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public Integer getDurationPlayed() { return durationPlayed; }
    public void setDurationPlayed(Integer durationPlayed) { this.durationPlayed = durationPlayed; }

    public boolean isFullPlay() { return isFullPlay; }
    public void setFullPlay(boolean fullPlay) { isFullPlay = fullPlay; }

    public String getReferrerUrl() { return referrerUrl; }
    public void setReferrerUrl(String referrerUrl) { this.referrerUrl = referrerUrl; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
