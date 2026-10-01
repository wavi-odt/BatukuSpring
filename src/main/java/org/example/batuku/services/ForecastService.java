package org.example.batuku.services;

import org.example.batuku.dto.ForecastResponse;
import org.example.batuku.dto.StatsResponse;
import org.example.batuku.repository.PlayRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForecastService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PlayRepository playRepository;
    private final GroqClient     groqClient;

    public ForecastService(PlayRepository playRepository, GroqClient groqClient) {
        this.playRepository = playRepository;
        this.groqClient     = groqClient;
    }

    public ForecastResponse forecastPlays(Long artistProfileId, int historyDays, int forecastDays) {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime since = now.minusDays(historyDays);

        // ── Dados históricos ──────────────────────────────────────────
        List<Object[]> raw = playRepository.findDailyPlaysByArtist(artistProfileId, since);
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (Object[] row : raw) {
            byDay.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        List<StatsResponse.DayCount> historical = new ArrayList<>();
        for (int i = historyDays - 1; i >= 0; i--) {
            String day = now.minusDays(i).toLocalDate().format(FMT);
            historical.add(new StatsResponse.DayCount(day, byDay.getOrDefault(day, 0L)));
        }

        // ── Regressão linear (mínimos quadrados) ─────────────────────
        int n = historical.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = historical.get(i).plays();
            sumX  += x;
            sumY  += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denom    = n * sumX2 - sumX * sumX;
        double slope    = denom != 0 ? (n * sumXY - sumX * sumY) / denom : 0;
        double intercept = (sumY - slope * sumX) / n;

        // ── Projeção ──────────────────────────────────────────────────
        List<StatsResponse.DayCount> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastDays; i++) {
            String day    = now.plusDays(i).toLocalDate().format(FMT);
            long   value  = Math.max(0, Math.round(slope * (n - 1 + i) + intercept));
            forecast.add(new StatsResponse.DayCount(day, value));
        }

        // ── Classificação de tendência ────────────────────────────────
        double avgHistorical = n > 0 ? sumY / n : 0;
        double pctChange     = avgHistorical > 0 ? (slope / avgHistorical) * 100.0 : 0;
        String trend;
        if      (pctChange > 5)  trend = "CRESCENTE";
        else if (pctChange < -5) trend = "DECRESCENTE";
        else                     trend = "ESTAVEL";

        // ── Insight via Groq (com fallback) ──────────────────────────
        long totalPlays = (long) sumY;
        double avgDaily = avgHistorical;

        String systemPrompt = "És um assistente de dados para artistas musicais. "
                + "Responde sempre em português europeu com uma única frase curta e direta, "
                + "sem markdown, sem inventar valores que não foram fornecidos.";

        String userPrompt = String.format(
                "O artista teve %d reproduções nos últimos %d dias (média %.1f/dia). "
                + "A tendência é %s (variação de %.1f%% relativamente à média). "
                + "Escreve uma frase de análise para o artista.",
                totalPlays, historyDays, avgDaily, trend, pctChange);

        String insight = groqClient.generateInsight(systemPrompt, userPrompt);
        if (insight == null) {
            insight = switch (trend) {
                case "CRESCENTE"   -> "As tuas reproduções estão a crescer, continua a publicar conteúdo regularmente.";
                case "DECRESCENTE" -> "As reproduções baixaram recentemente, considera promover as tuas faixas nas redes sociais.";
                default            -> "As tuas reproduções mantêm-se estáveis, um bom sinal de audiência fiel.";
            };
        }

        return new ForecastResponse(historical, forecast, trend, insight);
    }
}
