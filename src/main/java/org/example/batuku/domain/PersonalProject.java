package org.example.batuku.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personal_projects")
public class PersonalProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "share_token", nullable = false, unique = true, length = 36)
    private String shareToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Formula("(SELECT COUNT(*) FROM personal_project_tracks t WHERE t.project_id = id)")
    private int trackCount;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("created_at ASC")
    private List<PersonalProjectTrack> tracks = new ArrayList<>();

    protected PersonalProject() {}

    public PersonalProject(User user, String name, String coverUrl, String shareToken) {
        this.user       = user;
        this.name       = name;
        this.coverUrl   = coverUrl;
        this.shareToken = shareToken;
        this.createdAt  = LocalDateTime.now();
    }

    public Long getId()                           { return id; }
    public User getUser()                         { return user; }
    public String getName()                       { return name; }
    public String getCoverUrl()                   { return coverUrl; }
    public String getShareToken()                 { return shareToken; }
    public LocalDateTime getCreatedAt()           { return createdAt; }
    public int getTrackCount()                    { return trackCount; }
    public List<PersonalProjectTrack> getTracks() { return tracks; }

    public void setName(String name)              { this.name = name; }
    public void setCoverUrl(String coverUrl)      { this.coverUrl = coverUrl; }
    public void setShareToken(String token)       { this.shareToken = token; }
}
