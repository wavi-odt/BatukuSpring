package org.example.batuku.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "likes_enabled", nullable = false)
    private boolean likesEnabled = true;

    @Column(name = "follows_enabled", nullable = false)
    private boolean followsEnabled = true;

    @Column(name = "comments_enabled", nullable = false)
    private boolean commentsEnabled = true;

    @Column(name = "badges_enabled", nullable = false)
    private boolean badgesEnabled = true;

    @Column(name = "system_enabled", nullable = false)
    private boolean systemEnabled = true;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = false;

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isLikesEnabled() { return likesEnabled; }
    public void setLikesEnabled(boolean likesEnabled) { this.likesEnabled = likesEnabled; }

    public boolean isFollowsEnabled() { return followsEnabled; }
    public void setFollowsEnabled(boolean followsEnabled) { this.followsEnabled = followsEnabled; }

    public boolean isCommentsEnabled() { return commentsEnabled; }
    public void setCommentsEnabled(boolean commentsEnabled) { this.commentsEnabled = commentsEnabled; }

    public boolean isBadgesEnabled() { return badgesEnabled; }
    public void setBadgesEnabled(boolean badgesEnabled) { this.badgesEnabled = badgesEnabled; }

    public boolean isSystemEnabled() { return systemEnabled; }
    public void setSystemEnabled(boolean systemEnabled) { this.systemEnabled = systemEnabled; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
}
