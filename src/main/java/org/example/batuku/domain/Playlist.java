package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playlists")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_collaborative", nullable = false)
    private boolean isCollaborative = false;

    @Column(name = "is_system_generated", nullable = false)
    private boolean isSystemGenerated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCollaborative() { return isCollaborative; }
    public void setCollaborative(boolean collaborative) { isCollaborative = collaborative; }

    public boolean isSystemGenerated() { return isSystemGenerated; }
    public void setSystemGenerated(boolean systemGenerated) { isSystemGenerated = systemGenerated; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
