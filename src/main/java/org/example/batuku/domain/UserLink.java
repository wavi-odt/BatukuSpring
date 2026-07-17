package org.example.batuku.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_links")
public class UserLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkPlatform platform;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 100)
    private String label;

    public enum LinkPlatform { WEBSITE, INSTAGRAM, YOUTUBE, TWITTER, TIKTOK, SOUNDCLOUD, OTHER }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LinkPlatform getPlatform() { return platform; }
    public void setPlatform(LinkPlatform platform) { this.platform = platform; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
