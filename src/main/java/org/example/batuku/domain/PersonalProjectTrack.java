package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_project_tracks")
public class PersonalProjectTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private PersonalProject project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    protected PersonalProjectTrack() {}

    public PersonalProjectTrack(PersonalProject project, String title, String audioUrl, Integer durationSeconds) {
        this.project         = project;
        this.title           = title;
        this.audioUrl        = audioUrl;
        this.durationSeconds = durationSeconds;
        this.createdAt       = LocalDateTime.now();
    }

    public Long getId()                   { return id; }
    public PersonalProject getProject()   { return project; }
    public String getTitle()              { return title; }
    public String getAudioUrl()           { return audioUrl; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public Integer getDurationSeconds()   { return durationSeconds; }
}
