package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TargetType { TRACK, ARTIST, PLAYLIST }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
