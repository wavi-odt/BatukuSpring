package org.example.batuku.dto;

import java.util.List;
import java.util.Map;

public record StatsResponse(
        Map<String, KpiValue> kpis,
        List<DayCount>        dailyPlays,
        List<DayLike>         dailyLikes,
        List<DayFollower>     dailyFollowers,
        List<TrackStat>       topTracks,
        List<BreakdownItem>   sources,
        List<BreakdownItem>   locations,
        double                completionRate
) {
    public record KpiValue(long value, double delta, String label) {}
    public record DayCount(String day, long plays) {}
    public record DayLike(String day, long likes) {}
    public record DayFollower(String day, long followers) {}
    public record TrackStat(Long id, String title, String coverUrl, long plays, long likes) {}
    public record BreakdownItem(String label, long value, String color) {}
}
