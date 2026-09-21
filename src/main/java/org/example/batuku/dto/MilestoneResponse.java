package org.example.batuku.dto;

public class MilestoneResponse {

    private String label;
    private long   value;
    private String unit;

    public MilestoneResponse(String label, long value, String unit) {
        this.label = label;
        this.value = value;
        this.unit  = unit;
    }

    public String getLabel() { return label; }
    public long   getValue() { return value; }
    public String getUnit()  { return unit;  }
}
