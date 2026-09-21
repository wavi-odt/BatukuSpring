package org.example.batuku.dto;

import java.math.BigDecimal;

public class BeatEditRequest {
    private String title;
    private String genre;
    private Integer bpm;
    private String musicalKey;
    private Integer hue;
    private BigDecimal leasePrice;
    private BigDecimal premiumPrice;
    private BigDecimal exclusivePrice;
    private Boolean isNew;
    private Boolean isFeatured;
    private Boolean exclusiveNegotiable;

    public String getTitle()                    { return title; }
    public void setTitle(String v)              { this.title = v; }
    public String getGenre()                    { return genre; }
    public void setGenre(String v)              { this.genre = v; }
    public Integer getBpm()                     { return bpm; }
    public void setBpm(Integer v)               { this.bpm = v; }
    public String getMusicalKey()               { return musicalKey; }
    public void setMusicalKey(String v)         { this.musicalKey = v; }
    public Integer getHue()                     { return hue; }
    public void setHue(Integer v)               { this.hue = v; }
    public BigDecimal getLeasePrice()           { return leasePrice; }
    public void setLeasePrice(BigDecimal v)     { this.leasePrice = v; }
    public BigDecimal getPremiumPrice()         { return premiumPrice; }
    public void setPremiumPrice(BigDecimal v)   { this.premiumPrice = v; }
    public BigDecimal getExclusivePrice()       { return exclusivePrice; }
    public void setExclusivePrice(BigDecimal v) { this.exclusivePrice = v; }
    public Boolean getIsNew()                   { return isNew; }
    public void setIsNew(Boolean v)             { this.isNew = v; }
    public Boolean getIsFeatured()                  { return isFeatured; }
    public void setIsFeatured(Boolean v)            { this.isFeatured = v; }
    public Boolean getExclusiveNegotiable()         { return exclusiveNegotiable; }
    public void setExclusiveNegotiable(Boolean v)   { this.exclusiveNegotiable = v; }
}
