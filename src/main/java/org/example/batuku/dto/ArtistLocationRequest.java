package org.example.batuku.dto;

import jakarta.validation.constraints.NotBlank;

public class ArtistLocationRequest {

    @NotBlank
    private String location;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
