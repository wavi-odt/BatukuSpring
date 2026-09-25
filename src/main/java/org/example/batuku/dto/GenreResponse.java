package org.example.batuku.dto;

public class GenreResponse {

    private Long id;
    private String name;
    private int hue;
    private boolean caboverdean;
    private long trackCount;

    public GenreResponse(Long id, String name, int hue, boolean caboverdean, long trackCount) {
        this.id = id;
        this.name = name;
        this.hue = hue;
        this.caboverdean = caboverdean;
        this.trackCount = trackCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getHue() { return hue; }
    public boolean isCaboverdean() { return caboverdean; }
    public long getTrackCount() { return trackCount; }
}
