package org.example.batuku.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Nivel de fa de um artista (casual, regular, superfa). Valores por omissao:
 * pesos 1 (reproducao), 3 (gosto), 5 (comentario) e 10 (seguir);
 * limiares 30 (regular) e 100 (superfa). O peso de seguir conta sempre.
 */
class TierPropertiesTest {

    private final TierProperties tiers = new TierProperties();

    @ParameterizedTest(name = "{0} reproducoes, {1} gostos, {2} comentarios -> {3}")
    @CsvSource({
            "0,0,0,casual",       // so o peso de seguir: 10
            "19,0,0,casual",      // 19 + 10 = 29
            "20,0,0,regular",     // 20 + 10 = 30
            "0,6,0,casual",       // 18 + 10 = 28
            "0,7,0,regular",      // 21 + 10 = 31
            "0,0,4,regular",      // 20 + 10 = 30
            "89,0,0,regular",     // 99
            "90,0,0,superfan",    // 100
            "0,0,18,superfan",    // 90 + 10 = 100
            "10,10,10,superfan"   // 10 + 30 + 50 + 10 = 100
    })
    @DisplayName("Classifica o fa com os pesos e limiares por omissao")
    void classificaComValoresPorOmissao(long plays, long likes, long comments, String expected) {
        assertEquals(expected, tiers.computeTier(plays, likes, comments));
    }

    @Test
    @DisplayName("Os limiares e os pesos sao configuraveis")
    void respeitaConfiguracaoPersonalizada() {
        tiers.setWeightPlays(2);
        tiers.setThresholdRegular(50);
        tiers.setThresholdSuperfan(200);

        assertEquals("casual", tiers.computeTier(19, 0, 0));    // 38 + 10 = 48
        assertEquals("regular", tiers.computeTier(20, 0, 0));   // 40 + 10 = 50
        assertEquals("superfan", tiers.computeTier(95, 0, 0));  // 190 + 10 = 200
    }

    @Test
    @DisplayName("Sem peso de seguir, um fa sem atividade e casual")
    void semPesoDeSeguirFaSemAtividadeECasual() {
        tiers.setWeightFollows(0);
        assertEquals("casual", tiers.computeTier(0, 0, 0));
    }
}
