package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "unmapped_genres")
public class UnmappedGenre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int occurrences = 1;

    @Column(nullable = false)
    private LocalDateTime firstSeenAt;

    @Column
    private LocalDateTime lastSeenAt;

    public UnmappedGenre() {}

    public UnmappedGenre(String name) {
        this.name        = name;
        this.firstSeenAt = LocalDateTime.now();
        this.lastSeenAt  = this.firstSeenAt;
    }

    public Long getId()                 { return id; }
    public String getName()             { return name; }
    public int getOccurrences()         { return occurrences; }
    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public LocalDateTime getLastSeenAt()  { return lastSeenAt; }

    public void incrementOccurrences() {
        this.occurrences++;
        this.lastSeenAt = LocalDateTime.now();
    }
}
