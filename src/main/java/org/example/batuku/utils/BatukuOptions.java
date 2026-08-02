package org.example.batuku.utils;

import java.util.List;

/**
 * Listas canónicas de géneros, línguas e localizações válidos no Batuku.
 * Usadas para valida��ão nos endpoints de edição de perfil e para popular
 * o endpoint GET /api/artists/options (consumido pelo frontend).
 */
public final class BatukuOptions {

    private BatukuOptions() {}

    public static final List<String> GENRES = List.of(
            "Funaná", "Morna", "Coladeira", "Tabanka", "Cabo Love",
            "Batuque", "Batuco", "Kizomba", "Kolá San Jon", "Zouk",
            "Gospel", "Rap / Hip-hop", "R&B / Soul", "Reggae", "Electrónica"
    );

    public static final List<String> LANGUAGES = List.of(
            "Crioulo (CV)", "Português", "Inglês", "Francês",
            "Espanhol", "Holandês", "Alemão", "Italiano"
    );

    public record LocationOption(String value, String group) {}

    public static final List<LocationOption> LOCATIONS = List.of(
            // ── Santiago ──────────────────────────────────────────────
            new LocationOption("Praia, Santiago",        "Cabo Verde · Santiago"),
            new LocationOption("Assomada, Santiago",     "Cabo Verde · Santiago"),
            new LocationOption("Santa Cruz, Santiago",   "Cabo Verde · Santiago"),
            new LocationOption("Tarrafal, Santiago",     "Cabo Verde · Santiago"),
            // ── Fogo ──────────────────────────────────────────────────
            new LocationOption("São Filipe, Fogo",       "Cabo Verde · Fogo"),
            // ── São Vicente ───────────────────────────────────────────
            new LocationOption("Mindelo, São Vicente",   "Cabo Verde · São Vicente"),
            // ── Santo Antão ───────────────────────────────────────────
            new LocationOption("Ribeira Grande, Santo Antão", "Cabo Verde · Santo Antão"),
            new LocationOption("Ponta do Sol, Santo Antão",   "Cabo Verde · Santo Antão"),
            // ── São Nicolau ───────────────────────────────────────────
            new LocationOption("Ribeira Brava, São Nicolau",  "Cabo Verde · São Nicolau"),
            // ── Sal ───────────────────────────────────────────────────
            new LocationOption("Espargos, Sal",          "Cabo Verde · Sal"),
            new LocationOption("Santa Maria, Sal",       "Cabo Verde · Sal"),
            // ── Boa Vista ─────────────────────────────────────────────
            new LocationOption("Sal Rei, Boa Vista",     "Cabo Verde · Boa Vista"),
            // ── Maio ──────────────────────────────────────────────────
            new LocationOption("Vila do Maio, Maio",     "Cabo Verde · Maio"),
            // ── Brava ─────────────────────────────────────────────────
            new LocationOption("Nova Sintra, Brava",     "Cabo Verde · Brava"),
            // ── Diáspora ─��────────────────────────────────────────────
            new LocationOption("Lisboa, Portugal",       "Diáspora · Portugal"),
            new LocationOption("Porto, Portugal",        "Diáspora · Portugal"),
            new LocationOption("Setúbal, Portugal",      "Diáspora · Portugal"),
            new LocationOption("Roterdão, Países Baixos", "Diáspora · Países Baixos"),
            new LocationOption("Amesterdão, Países Baixos", "Diáspora · Países Baixos"),
            new LocationOption("Haia, Países Baixos",    "Diáspora · Países Baixos"),
            new LocationOption("Boston, EUA",            "Diáspora · EUA"),
            new LocationOption("Providence, EUA",        "Diáspora · EUA"),
            new LocationOption("Nova Iorque, EUA",       "Diáspora · EUA"),
            new LocationOption("Paris, França",          "Diáspora · França"),
            new LocationOption("Marselha, França",       "Diáspora · França"),
            new LocationOption("Milão, Itália",          "Diáspora · Itália"),
            new LocationOption("Luxemburgo, Luxemburgo", "Diáspora · Luxemburgo")
    );

    public static boolean isValidLocation(String value) {
        return LOCATIONS.stream().anyMatch(l -> l.value().equals(value));
    }

    public static boolean isValidGenre(String value) {
        return GENRES.contains(value);
    }

    public static boolean isValidLanguage(String value) {
        return LANGUAGES.contains(value);
    }
}
