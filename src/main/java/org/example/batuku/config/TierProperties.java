package org.example.batuku.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "batuku.tier")
public class TierProperties {

    private int weightPlays      = 1;
    private int weightLikes      = 3;
    private int weightComments   = 5;
    private int weightFollows    = 10;
    private int thresholdRegular  = 30;
    private int thresholdSuperfan = 100;

    public String computeTier(long plays, long likes, long comments) {
        long score = plays    * weightPlays
                   + likes    * weightLikes
                   + comments * weightComments
                   + weightFollows;
        if (score >= thresholdSuperfan) return "superfan";
        if (score >= thresholdRegular)  return "regular";
        return "casual";
    }

    public int getWeightPlays()        { return weightPlays; }
    public int getWeightLikes()        { return weightLikes; }
    public int getWeightComments()     { return weightComments; }
    public int getWeightFollows()      { return weightFollows; }
    public int getThresholdRegular()   { return thresholdRegular; }
    public int getThresholdSuperfan()  { return thresholdSuperfan; }

    public void setWeightPlays(int v)        { this.weightPlays = v; }
    public void setWeightLikes(int v)        { this.weightLikes = v; }
    public void setWeightComments(int v)     { this.weightComments = v; }
    public void setWeightFollows(int v)      { this.weightFollows = v; }
    public void setThresholdRegular(int v)   { this.thresholdRegular = v; }
    public void setThresholdSuperfan(int v)  { this.thresholdSuperfan = v; }
}
