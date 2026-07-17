package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_points")
public class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "experience_points", nullable = false)
    private Integer experiencePoints = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(Integer experiencePoints) { this.experiencePoints = experiencePoints; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
