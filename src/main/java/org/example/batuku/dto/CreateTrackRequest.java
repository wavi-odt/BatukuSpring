package org.example.batuku.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTrackRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String audioUrl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}
