package org.example.batuku.dto;

public class PurchaseRequest {
    private Long beatId;
    private String licenseType;

    public Long getBeatId()              { return beatId; }
    public void setBeatId(Long beatId)   { this.beatId = beatId; }
    public String getLicenseType()       { return licenseType; }
    public void setLicenseType(String l) { this.licenseType = l; }
}
