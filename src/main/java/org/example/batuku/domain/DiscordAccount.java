package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discord_accounts")
public class DiscordAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "discord_user_id", nullable = false, unique = true)
    private String discordUserId;

    @Column(name = "discord_username", nullable = false)
    private String discordUsername;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDiscordUserId() { return discordUserId; }
    public void setDiscordUserId(String discordUserId) { this.discordUserId = discordUserId; }

    public String getDiscordUsername() { return discordUsername; }
    public void setDiscordUsername(String discordUsername) { this.discordUsername = discordUsername; }

    public LocalDateTime getLinkedAt() { return linkedAt; }
}
