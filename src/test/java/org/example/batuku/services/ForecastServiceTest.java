package org.example.batuku.services;

import org.example.batuku.dto.ForecastResponse;
import org.example.batuku.repository.PlayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Previsao de reproducoes: regressao linear (minimos quadrados) sobre os
 * ultimos 30 dias e classificacao da tendencia (limite de 5 %).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForecastServiceTest {

    private static final long ARTIST_ID = 1L;

    @Mock PlayRepository playRepository;
    @Mock GroqClient groqClient;
    @InjectMocks ForecastService service;

    /** Serie diaria de {@code days} dias, do mais antigo (k = 0) ao de hoje. */
    private void historico(int days, IntUnaryOperator playsPorIndice) {
        List<Object[]> rows = new ArrayList<>();
        for (int k = 0; k < days; k++) {
            String dia = LocalDate.now().minusDays(days - 1 - k).toString();
            rows.add(new Object[]{dia, Long.valueOf(playsPorIndice.applyAsInt(k))});
        }
        when(playRepository.findDailyPlaysByArtist(eq(ARTIST_ID), any(LocalDateTime.class))).thenReturn(rows);
    }

    @Test
    @DisplayName("Serie crescente: tendencia CRESCENTE e projecao na mesma reta")
    void serieCrescente() {
        historico(30, k -> 10 + 5 * k);   // declive 5, media 82,5 -> +6,1 %

        ForecastResponse r = service.forecastPlays(ARTIST_ID, 30, 7);

        assertEquals("CRESCENTE", r.trend());
        assertEquals(30, r.historical().size());
        assertEquals(7, r.forecast().size());
        assertEquals(10, r.historical().get(0).plays());
        assertEquals(155, r.historical().get(29).plays());
        assertEquals(160, r.forecast().get(0).plays());   // 10 + 5 * 30
        assertEquals(190, r.forecast().get(6).plays());   // 10 + 5 * 36
    }

    @Test
    @DisplayName("Serie decrescente: tendencia DECRESCENTE e projecao nunca negativa")
    void serieDecrescente() {
        historico(30, k -> 160 - 5 * k);  // declive -5, media 87,5 -> -5,7 %

        ForecastResponse r = service.forecastPlays(ARTIST_ID, 30, 7);

        assertEquals("DECRESCENTE", r.trend());
        assertEquals(10, r.forecast().get(0).plays());    // 160 - 5 * 30
        assertEquals(0, r.forecast().get(3).plays());     // a reta daria -5
        assertEquals(0, r.forecast().get(6).plays());     // a reta daria -20
        assertTrue(r.forecast().stream().allMatch(d -> d.plays() >= 0));
    }

    @Test
    @DisplayName("Serie constante: tendencia ESTAVEL e projecao igual ao historico")
    void serieConstante() {
        historico(30, k -> 20);

        ForecastResponse r = service.forecastPlays(ARTIST_ID, 30, 7);

        assertEquals("ESTAVEL", r.trend());
        assertTrue(r.forecast().stream().allMatch(d -> d.plays() == 20));
    }

    @Test
    @DisplayName("Variacao abaixo de 5 % continua a ser ESTAVEL")
    void variacaoPequenaEEstavel() {
        historico(30, k -> 1000 + k);     // declive 1 sobre media 1014,5 -> 0,1 %

        assertEquals("ESTAVEL", service.forecastPlays(ARTIST_ID, 30, 7).trend());
    }

    @Test
    @DisplayName("Sem reproducoes: historico preenchido com zeros e projecao a zero")
    void semReproducoes() {
        when(playRepository.findDailyPlaysByArtist(eq(ARTIST_ID), any(LocalDateTime.class)))
                .thenReturn(List.of());

        ForecastResponse r = service.forecastPlays(ARTIST_ID, 30, 7);

        assertEquals("ESTAVEL", r.trend());
        assertEquals(30, r.historical().size());
        assertTrue(r.historical().stream().allMatch(d -> d.plays() == 0));
        assertTrue(r.forecast().stream().allMatch(d -> d.plays() == 0));
    }

    @Test
    @DisplayName("Os dias do historico terminam hoje e os da projecao comecam amanha")
    void datasDoHistoricoEDaProjecao() {
        historico(30, k -> 10);

        ForecastResponse r = service.forecastPlays(ARTIST_ID, 30, 7);

        assertEquals(LocalDate.now().toString(), r.historical().get(29).day());
        assertEquals(LocalDate.now().minusDays(29).toString(), r.historical().get(0).day());
        assertEquals(LocalDate.now().plusDays(1).toString(), r.forecast().get(0).day());
        assertEquals(LocalDate.now().plusDays(7).toString(), r.forecast().get(6).day());
    }

    @Test
    @DisplayName("Usa a frase do modelo de linguagem quando este responde")
    void usaFraseDoModeloDeLinguagem() {
        historico(30, k -> 10 + 5 * k);
        when(groqClient.generateInsight(anyString(), anyString())).thenReturn("Frase gerada pelo modelo.");

        ForecastResponse r = service.forecastPlays(ARTIST_ID, 30, 7);

        assertEquals("Frase gerada pelo modelo.", r.insight());
        verify(groqClient).generateInsight(anyString(), contains("CRESCENTE"));
    }

    @Test
    @DisplayName("Sem resposta do modelo, usa um texto alternativo adequado a tendencia")
    void usaTextoAlternativoSemModelo() {
        when(groqClient.generateInsight(anyString(), anyString())).thenReturn(null);

        historico(30, k -> 10 + 5 * k);
        assertTrue(service.forecastPlays(ARTIST_ID, 30, 7).insight().contains("crescer"));

        historico(30, k -> 160 - 5 * k);
        assertTrue(service.forecastPlays(ARTIST_ID, 30, 7).insight().contains("baixaram"));

        historico(30, k -> 20);
        assertTrue(service.forecastPlays(ARTIST_ID, 30, 7).insight().contains("fiel"));
    }
}
