package org.example.batuku.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String value;

    @Column(nullable = false)
    private String locationGroup;

    public Long getId() { return id; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getLocationGroup() { return locationGroup; }
    public void setLocationGroup(String locationGroup) { this.locationGroup = locationGroup; }
}
