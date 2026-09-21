package org.example.batuku.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BeatResponse {

    private Long id;
    private String title;
    private String producer;
    private Long producerId;
    private String genre;
    private Integer bpm;
    private String key;
    private Integer hue;
    private String image;
    private String audioUrl;
    private Prices prices;
    private Integer plays;
    private Integer sales;
    private boolean isNew;
    private boolean isFeatured;
    private boolean exclusiveNegotiable;
    private String desc;

    public static class Prices {
        private double lease;
        private double premium;
        private double exclusive;

        public double getLease()    { return lease; }
        public void setLease(double lease) { this.lease = lease; }
        public double getPremium()  { return premium; }
        public void setPremium(double premium) { this.premium = premium; }
        public double getExclusive() { return exclusive; }
        public void setExclusive(double exclusive) { this.exclusive = exclusive; }
    }

    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public String getTitle()            { return title; }
    public void setTitle(String t)      { this.title = t; }
    public String getProducer()         { return producer; }
    public void setProducer(String p)   { this.producer = p; }
    public Long getProducerId()         { return producerId; }
    public void setProducerId(Long p)   { this.producerId = p; }
    public String getGenre()            { return genre; }
    public void setGenre(String g)      { this.genre = g; }
    public Integer getBpm()             { return bpm; }
    public void setBpm(Integer b)       { this.bpm = b; }
    public String getKey()              { return key; }
    public void setKey(String k)        { this.key = k; }
    public Integer getHue()             { return hue; }
    public void setHue(Integer h)       { this.hue = h; }
    public String getImage()            { return image; }
    public void setImage(String i)      { this.image = i; }
    public String getAudioUrl()         { return audioUrl; }
    public void setAudioUrl(String u)   { this.audioUrl = u; }
    public Prices getPrices()           { return prices; }
    public void setPrices(Prices p)     { this.prices = p; }
    public Integer getPlays()           { return plays; }
    public void setPlays(Integer p)     { this.plays = p; }
    public Integer getSales()           { return sales; }
    public void setSales(Integer s)     { this.sales = s; }
    @JsonProperty("isNew")
    public boolean isNew()              { return isNew; }
    public void setNew(boolean n)       { this.isNew = n; }
    @JsonProperty("isFeatured")
    public boolean isFeatured()                         { return isFeatured; }
    public void setFeatured(boolean f)                  { this.isFeatured = f; }
    public boolean isExclusiveNegotiable()              { return exclusiveNegotiable; }
    public void setExclusiveNegotiable(boolean n)       { this.exclusiveNegotiable = n; }
    public String getDesc()                             { return desc; }
    public void setDesc(String d)                       { this.desc = d; }
}
