package org.example.batuku.dto;

import jakarta.validation.constraints.Size;

public class ArtistBioRequest {

    @Size(max = 1000)
    private String bio;

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
