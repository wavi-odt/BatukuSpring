package org.example.batuku.config;

import org.example.batuku.domain.Location;
import org.example.batuku.repository.LocationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class SeedLocations {

    @Bean
    @Order(8)
    CommandLineRunner seedLocationsRunner(LocationRepository locationRepository) {
        return args -> {
            // Santiago
            seed(locationRepository, "Praia, Santiago",        "Cabo Verde · Santiago");
            seed(locationRepository, "Assomada, Santiago",     "Cabo Verde · Santiago");
            seed(locationRepository, "Santa Cruz, Santiago",   "Cabo Verde · Santiago");
            seed(locationRepository, "Tarrafal, Santiago",     "Cabo Verde · Santiago");
            // Fogo
            seed(locationRepository, "São Filipe, Fogo",       "Cabo Verde · Fogo");
            // São Vicente
            seed(locationRepository, "Mindelo, São Vicente",   "Cabo Verde · São Vicente");
            // Santo Antão
            seed(locationRepository, "Ribeira Grande, Santo Antão", "Cabo Verde · Santo Antão");
            seed(locationRepository, "Ponta do Sol, Santo Antão",   "Cabo Verde · Santo Antão");
            // São Nicolau
            seed(locationRepository, "Ribeira Brava, São Nicolau",  "Cabo Verde · São Nicolau");
            // Sal
            seed(locationRepository, "Espargos, Sal",          "Cabo Verde · Sal");
            seed(locationRepository, "Santa Maria, Sal",       "Cabo Verde · Sal");
            // Boa Vista
            seed(locationRepository, "Sal Rei, Boa Vista",     "Cabo Verde · Boa Vista");
            // Maio
            seed(locationRepository, "Vila do Maio, Maio",     "Cabo Verde · Maio");
            // Brava
            seed(locationRepository, "Nova Sintra, Brava",     "Cabo Verde · Brava");
            // Diáspora
            seed(locationRepository, "Lisboa, Portugal",           "Diáspora · Portugal");
            seed(locationRepository, "Porto, Portugal",            "Diáspora · Portugal");
            seed(locationRepository, "Setúbal, Portugal",          "Diáspora · Portugal");
            seed(locationRepository, "Roterdão, Países Baixos",    "Diáspora · Países Baixos");
            seed(locationRepository, "Amesterdão, Países Baixos",  "Diáspora · Países Baixos");
            seed(locationRepository, "Haia, Países Baixos",        "Diáspora · Países Baixos");
            seed(locationRepository, "Boston, EUA",                "Diáspora · EUA");
            seed(locationRepository, "Providence, EUA",            "Diáspora · EUA");
            seed(locationRepository, "Nova Iorque, EUA",           "Diáspora · EUA");
            seed(locationRepository, "Paris, França",              "Diáspora · França");
            seed(locationRepository, "Marselha, França",           "Diáspora · França");
            seed(locationRepository, "Milão, Itália",              "Diáspora · Itália");
            seed(locationRepository, "Luxemburgo, Luxemburgo",     "Diáspora · Luxemburgo");

            System.out.println("Localizações do Batuku verificadas/criadas.");
        };
    }

    private void seed(LocationRepository repo, String value, String group) {
        if (!repo.existsByValue(value)) {
            Location l = new Location();
            l.setValue(value);
            l.setLocationGroup(group);
            repo.save(l);
        }
    }
}
