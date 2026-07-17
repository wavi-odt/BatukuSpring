package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discord_events")
public class DiscordEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "discord_account_id", nullable = false)
    private DiscordAccount discordAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EventType { MESSAGE, SHARE, REACTION, JOIN }

    public Long getId() { return id; }

    public DiscordAccount getDiscordAccount() { return discordAccount; }
    public void setDiscordAccount(DiscordAccount discordAccount) { this.discordAccount = discordAccount; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
