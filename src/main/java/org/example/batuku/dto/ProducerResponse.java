package org.example.batuku.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProducerResponse {

    private Long id;
    private String name;
    private String handle;
    private String genre;
    private long beats;
    private long sales;
    private double rating;
    private int hue;
    private String shape;
    private String image;
    private boolean isVerified;

    public Long getId()               { return id; }
    public void setId(Long id)        { this.id = id; }
    public String getName()           { return name; }
    public void setName(String n)     { this.name = n; }
    public String getHandle()         { return handle; }
    public void setHandle(String h)   { this.handle = h; }
    public String getGenre()          { return genre; }
    public void setGenre(String g)    { this.genre = g; }
    public long getBeats()            { return beats; }
    public void setBeats(long b)      { this.beats = b; }
    public long getSales()            { return sales; }
    public void setSales(long s)      { this.sales = s; }
    public double getRating()         { return rating; }
    public void setRating(double r)   { this.rating = r; }
    public int getHue()               { return hue; }
    public void setHue(int h)         { this.hue = h; }
    public String getShape()          { return shape; }
    public void setShape(String s)    { this.shape = s; }
    public String getImage()          { return image; }
    public void setImage(String i)    { this.image = i; }
    @JsonProperty("isVerified")
    public boolean isVerified()       { return isVerified; }
    public void setVerified(boolean v){ this.isVerified = v; }
}
