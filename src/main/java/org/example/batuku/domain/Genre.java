package org.example.batuku.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int hue;

    @Column(nullable = false)
    private boolean caboverdean;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getHue() { return hue; }
    public void setHue(int hue) { this.hue = hue; }

    public boolean isCaboverdean() { return caboverdean; }
    public void setCaboverdean(boolean caboverdean) { this.caboverdean = caboverdean; }
}
