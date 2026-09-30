package org.example.batuku.dto;

import java.util.List;

public record ForecastResponse(
        List<StatsResponse.DayCount> historical,
        List<StatsResponse.DayCount> forecast,
        String trend,
        String insight
) {}
