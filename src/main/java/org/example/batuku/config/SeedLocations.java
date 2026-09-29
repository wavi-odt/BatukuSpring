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
            seed(locationRepository, "Amadora, Portugal",          "Diáspora · Portugal");
            seed(locationRepository, "Almada, Portugal",           "Diáspora · Portugal");
            seed(locationRepository, "Loures, Portugal",           "Diáspora · Portugal");
            seed(locationRepository, "Roterdão, Países Baixos",    "Diáspora · Países Baixos");
            seed(locationRepository, "Amesterdão, Países Baixos",  "Diáspora · Países Baixos");
            seed(locationRepository, "Haia, Países Baixos",        "Diáspora · Países Baixos");
            seed(locationRepository, "Boston, EUA",                "Diáspora · EUA");
            seed(locationRepository, "Providence, EUA",            "Diáspora · EUA");
            seed(locationRepository, "Nova Iorque, EUA",           "Diáspora · EUA");
            seed(locationRepository, "New Bedford, EUA",           "Diáspora · EUA");
            seed(locationRepository, "Brockton, EUA",              "Diáspora · EUA");
            seed(locationRepository, "Paris, França",              "Diáspora · França");
            seed(locationRepository, "Marselha, França",           "Diáspora · França");
            seed(locationRepository, "Lyon, França",               "Diáspora · França");
            seed(locationRepository, "Milão, Itália",              "Diáspora · Itália");
            seed(locationRepository, "Roma, Itália",               "Diáspora · Itália");
            seed(locationRepository, "Génova, Itália",             "Diáspora · Itália");
            seed(locationRepository, "Torino, Itália",             "Diáspora · Itália");
            seed(locationRepository, "Luxemburgo, Luxemburgo",     "Diáspora · Luxemburgo");
            seed(locationRepository, "Londres, Reino Unido",       "Diáspora · Reino Unido");
            seed(locationRepository, "Madrid, Espanha",            "Diáspora · Espanha");
            seed(locationRepository, "Barcelona, Espanha",         "Diáspora · Espanha");
            seed(locationRepository, "Bruxelas, Bélgica",          "Diáspora · Bélgica");
            seed(locationRepository, "Luanda, Angola",             "Diáspora · Angola");

            // ── África ──────────────────────────────────────────────────────
            seed(locationRepository, "Dakar, Senegal",                  "África");
            seed(locationRepository, "Bissau, Guiné-Bissau",            "África");
            seed(locationRepository, "Conacri, Guiné",                  "África");
            seed(locationRepository, "Freetown, Serra Leoa",            "África");
            seed(locationRepository, "Monróvia, Libéria",               "África");
            seed(locationRepository, "Abidjan, Costa do Marfim",        "África");
            seed(locationRepository, "Yamoussoukro, Costa do Marfim",   "África");
            seed(locationRepository, "Acra, Gana",                      "África");
            seed(locationRepository, "Lomé, Togo",                      "África");
            seed(locationRepository, "Cotonou, Benim",                  "África");
            seed(locationRepository, "Porto Novo, Benim",               "África");
            seed(locationRepository, "Lagos, Nigéria",                  "África");
            seed(locationRepository, "Abuja, Nigéria",                  "África");
            seed(locationRepository, "Bamako, Mali",                    "África");
            seed(locationRepository, "Uagadugu, Burkina Faso",          "África");
            seed(locationRepository, "Niamei, Níger",                   "África");
            seed(locationRepository, "Banjul, Gâmbia",                  "África");
            seed(locationRepository, "Nouakchott, Mauritânia",          "África");
            seed(locationRepository, "Ndjamena, Chade",                 "África");
            seed(locationRepository, "Bangui, Rep. Centro-Africana",    "África");
            seed(locationRepository, "Yaoundé, Camarões",               "África");
            seed(locationRepository, "Malabo, Guiné Equatorial",        "África");
            seed(locationRepository, "São Tomé, São Tomé e Príncipe",   "África");
            seed(locationRepository, "Libreville, Gabão",               "África");
            seed(locationRepository, "Brazzaville, Congo",              "África");
            seed(locationRepository, "Quinxassa, Congo (RDC)",          "África");
            seed(locationRepository, "Kigali, Ruanda",                  "África");
            seed(locationRepository, "Bujumbura, Burundi",              "África");
            seed(locationRepository, "Kampala, Uganda",                 "África");
            seed(locationRepository, "Nairóbi, Quénia",                 "África");
            seed(locationRepository, "Dar es Salaam, Tanzânia",         "África");
            seed(locationRepository, "Dodoma, Tanzânia",                "África");
            seed(locationRepository, "Moroni, Comores",                 "África");
            seed(locationRepository, "Adis Abeba, Etiópia",             "África");
            seed(locationRepository, "Asmara, Eritreia",                "África");
            seed(locationRepository, "Djibuti, Djibuti",                "África");
            seed(locationRepository, "Mogadíscio, Somália",             "África");
            seed(locationRepository, "Juba, Sudão do Sul",              "África");
            seed(locationRepository, "Cartum, Sudão",                   "África");
            seed(locationRepository, "Cairo, Egito",                    "África");
            seed(locationRepository, "Alexandria, Egito",               "África");
            seed(locationRepository, "Trípoli, Líbia",                  "África");
            seed(locationRepository, "Tunes, Tunísia",                  "África");
            seed(locationRepository, "Argel, Argélia",                  "África");
            seed(locationRepository, "Rabat, Marrocos",                 "África");
            seed(locationRepository, "Casablanca, Marrocos",            "África");
            seed(locationRepository, "Lilongwe, Malawi",                "África");
            seed(locationRepository, "Lusaka, Zâmbia",                  "África");
            seed(locationRepository, "Maputo, Moçambique",              "África");
            seed(locationRepository, "Harare, Zimbabwe",                "África");
            seed(locationRepository, "Antananarivo, Madagáscar",        "África");
            seed(locationRepository, "Joanesburgo, África do Sul",      "África");
            seed(locationRepository, "Cidade do Cabo, África do Sul",   "África");
            seed(locationRepository, "Durban, África do Sul",           "África");
            seed(locationRepository, "Windhoek, Namíbia",               "África");
            seed(locationRepository, "Gaborone, Botswana",              "África");
            seed(locationRepository, "Mbabane, Essuatíni",              "África");
            seed(locationRepository, "Maseru, Lesoto",                  "África");

            // ── Europa ──────────────────────────────────────────────────────
            seed(locationRepository, "Berlim, Alemanha",                "Europa");
            seed(locationRepository, "Frankfurt, Alemanha",             "Europa");
            seed(locationRepository, "Hamburgo, Alemanha",              "Europa");
            seed(locationRepository, "Munique, Alemanha",               "Europa");
            seed(locationRepository, "Colónia, Alemanha",               "Europa");
            seed(locationRepository, "Viena, Áustria",                  "Europa");
            seed(locationRepository, "Zurique, Suíça",                  "Europa");
            seed(locationRepository, "Genebra, Suíça",                  "Europa");
            seed(locationRepository, "Estocolmo, Suécia",               "Europa");
            seed(locationRepository, "Gotemburgo, Suécia",              "Europa");
            seed(locationRepository, "Oslo, Noruega",                   "Europa");
            seed(locationRepository, "Copenhaga, Dinamarca",            "Europa");
            seed(locationRepository, "Helsínquia, Finlândia",           "Europa");
            seed(locationRepository, "Dublin, Irlanda",                 "Europa");
            seed(locationRepository, "Varsóvia, Polónia",               "Europa");
            seed(locationRepository, "Cracóvia, Polónia",               "Europa");
            seed(locationRepository, "Praga, Rep. Checa",               "Europa");
            seed(locationRepository, "Bratislava, Eslováquia",          "Europa");
            seed(locationRepository, "Budapeste, Hungria",              "Europa");
            seed(locationRepository, "Bucareste, Roménia",              "Europa");
            seed(locationRepository, "Sófia, Bulgária",                 "Europa");
            seed(locationRepository, "Atenas, Grécia",                  "Europa");
            seed(locationRepository, "Zagreb, Croácia",                 "Europa");
            seed(locationRepository, "Liubliana, Eslovénia",            "Europa");
            seed(locationRepository, "Sarajevo, Bósnia-Herzegovina",    "Europa");
            seed(locationRepository, "Belgrado, Sérvia",                "Europa");
            seed(locationRepository, "Podgorica, Montenegro",           "Europa");
            seed(locationRepository, "Skopje, Macedónia do Norte",      "Europa");
            seed(locationRepository, "Tirana, Albânia",                 "Europa");
            seed(locationRepository, "Pristina, Kosovo",                "Europa");
            seed(locationRepository, "Chisinau, Moldávia",              "Europa");
            seed(locationRepository, "Kiev, Ucrânia",                   "Europa");
            seed(locationRepository, "Kharkiv, Ucrânia",                "Europa");
            seed(locationRepository, "Moscovo, Rússia",                 "Europa");
            seed(locationRepository, "São Petersburgo, Rússia",         "Europa");
            seed(locationRepository, "Minsk, Bielorrússia",             "Europa");
            seed(locationRepository, "Vilnius, Lituânia",               "Europa");
            seed(locationRepository, "Riga, Letónia",                   "Europa");
            seed(locationRepository, "Tallin, Estónia",                 "Europa");
            seed(locationRepository, "Tbilisi, Geórgia",                "Europa");
            seed(locationRepository, "Erevan, Arménia",                 "Europa");
            seed(locationRepository, "Baku, Azerbaijão",                "Europa");
            seed(locationRepository, "Reiquiavique, Islândia",          "Europa");
            seed(locationRepository, "Nicósia, Chipre",                 "Europa");
            seed(locationRepository, "Valeta, Malta",                   "Europa");
            seed(locationRepository, "Andorra a Velha, Andorra",        "Europa");

            // ── América do Norte ────────────────────────────────────────────
            seed(locationRepository, "Toronto, Canadá",                 "América do Norte");
            seed(locationRepository, "Montreal, Canadá",                "América do Norte");
            seed(locationRepository, "Vancouver, Canadá",               "América do Norte");
            seed(locationRepository, "Calgary, Canadá",                 "América do Norte");
            seed(locationRepository, "Ottawa, Canadá",                  "América do Norte");
            seed(locationRepository, "Cidade do México, México",        "América do Norte");
            seed(locationRepository, "Guadalajara, México",             "América do Norte");
            seed(locationRepository, "Monterrey, México",               "América do Norte");
            seed(locationRepository, "Miami, EUA",                      "América do Norte");
            seed(locationRepository, "Los Angeles, EUA",                "América do Norte");
            seed(locationRepository, "Chicago, EUA",                    "América do Norte");
            seed(locationRepository, "Houston, EUA",                    "América do Norte");
            seed(locationRepository, "Washington, EUA",                 "América do Norte");
            seed(locationRepository, "Atlanta, EUA",                    "América do Norte");
            seed(locationRepository, "Dallas, EUA",                     "América do Norte");
            seed(locationRepository, "San Francisco, EUA",              "América do Norte");
            seed(locationRepository, "Seattle, EUA",                    "América do Norte");
            seed(locationRepository, "Phoenix, EUA",                    "América do Norte");
            seed(locationRepository, "Minneapolis, EUA",                "América do Norte");

            // ── América Central e Caraíbas ──────────────────────────────────
            seed(locationRepository, "Cidade da Guatemala, Guatemala",  "América Central e Caraíbas");
            seed(locationRepository, "Tegucigalpa, Honduras",           "América Central e Caraíbas");
            seed(locationRepository, "San Salvador, El Salvador",       "América Central e Caraíbas");
            seed(locationRepository, "Manágua, Nicarágua",              "América Central e Caraíbas");
            seed(locationRepository, "São José, Costa Rica",            "América Central e Caraíbas");
            seed(locationRepository, "Cidade do Panamá, Panamá",        "América Central e Caraíbas");
            seed(locationRepository, "Belmopán, Belize",                "América Central e Caraíbas");
            seed(locationRepository, "Havana, Cuba",                    "América Central e Caraíbas");
            seed(locationRepository, "Santo Domingo, Rep. Dominicana",  "América Central e Caraíbas");
            seed(locationRepository, "Porto Príncipe, Haiti",           "América Central e Caraíbas");
            seed(locationRepository, "Kingston, Jamaica",               "América Central e Caraíbas");
            seed(locationRepository, "Porto de Espanha, Trinidad e Tobago", "América Central e Caraíbas");
            seed(locationRepository, "San Juan, Porto Rico",            "América Central e Caraíbas");
            seed(locationRepository, "Nassau, Bahamas",                 "América Central e Caraíbas");
            seed(locationRepository, "Bridgetown, Barbados",            "América Central e Caraíbas");
            seed(locationRepository, "Georgetown, Guiana",              "América Central e Caraíbas");
            seed(locationRepository, "Paramaribo, Suriname",            "América Central e Caraíbas");

            // ── América do Sul ───────────────────────────────────────────────
            seed(locationRepository, "São Paulo, Brasil",               "América do Sul");
            seed(locationRepository, "Rio de Janeiro, Brasil",          "América do Sul");
            seed(locationRepository, "Brasília, Brasil",                "América do Sul");
            seed(locationRepository, "Salvador, Brasil",                "América do Sul");
            seed(locationRepository, "Fortaleza, Brasil",               "América do Sul");
            seed(locationRepository, "Belo Horizonte, Brasil",          "América do Sul");
            seed(locationRepository, "Manaus, Brasil",                  "América do Sul");
            seed(locationRepository, "Buenos Aires, Argentina",         "América do Sul");
            seed(locationRepository, "Córdoba, Argentina",              "América do Sul");
            seed(locationRepository, "Rosário, Argentina",              "América do Sul");
            seed(locationRepository, "Santiago, Chile",                 "América do Sul");
            seed(locationRepository, "Valparaíso, Chile",               "América do Sul");
            seed(locationRepository, "Lima, Peru",                      "América do Sul");
            seed(locationRepository, "Bogotá, Colômbia",                "América do Sul");
            seed(locationRepository, "Medellín, Colômbia",              "América do Sul");
            seed(locationRepository, "Cali, Colômbia",                  "América do Sul");
            seed(locationRepository, "Caracas, Venezuela",              "América do Sul");
            seed(locationRepository, "Quito, Equador",                  "América do Sul");
            seed(locationRepository, "Guayaquil, Equador",              "América do Sul");
            seed(locationRepository, "La Paz, Bolívia",                 "América do Sul");
            seed(locationRepository, "Assunção, Paraguai",              "América do Sul");
            seed(locationRepository, "Montevidéu, Uruguai",             "América do Sul");

            // ── Ásia e Médio Oriente ─────────────────────────────────────────
            seed(locationRepository, "Istambul, Turquia",               "Ásia e Médio Oriente");
            seed(locationRepository, "Ancara, Turquia",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Amã, Jordânia",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Beirute, Líbano",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Damasco, Síria",                  "Ásia e Médio Oriente");
            seed(locationRepository, "Bagdade, Iraque",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Teerão, Irão",                    "Ásia e Médio Oriente");
            seed(locationRepository, "Tel Aviv, Israel",                "Ásia e Médio Oriente");
            seed(locationRepository, "Dubai, Emirados Árabes Unidos",   "Ásia e Médio Oriente");
            seed(locationRepository, "Abu Dhabi, Emirados Árabes Unidos", "Ásia e Médio Oriente");
            seed(locationRepository, "Riade, Arábia Saudita",           "Ásia e Médio Oriente");
            seed(locationRepository, "Jidá, Arábia Saudita",            "Ásia e Médio Oriente");
            seed(locationRepository, "Doha, Qatar",                     "Ásia e Médio Oriente");
            seed(locationRepository, "Kuwait, Kuwait",                  "Ásia e Médio Oriente");
            seed(locationRepository, "Manama, Bahrein",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Mascate, Omã",                    "Ásia e Médio Oriente");
            seed(locationRepository, "Saná, Iémen",                     "Ásia e Médio Oriente");
            seed(locationRepository, "Cabul, Afeganistão",              "Ásia e Médio Oriente");
            seed(locationRepository, "Islamabad, Paquistão",            "Ásia e Médio Oriente");
            seed(locationRepository, "Karachi, Paquistão",              "Ásia e Médio Oriente");
            seed(locationRepository, "Lahore, Paquistão",               "Ásia e Médio Oriente");
            seed(locationRepository, "Nova Deli, Índia",                "Ásia e Médio Oriente");
            seed(locationRepository, "Mumbai, Índia",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Bangalore, Índia",                "Ásia e Médio Oriente");
            seed(locationRepository, "Chennai, Índia",                  "Ásia e Médio Oriente");
            seed(locationRepository, "Kolkata, Índia",                  "Ásia e Médio Oriente");
            seed(locationRepository, "Daca, Bangladesh",                "Ásia e Médio Oriente");
            seed(locationRepository, "Colombo, Sri Lanka",              "Ásia e Médio Oriente");
            seed(locationRepository, "Katmandu, Nepal",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Almaty, Cazaquistão",             "Ásia e Médio Oriente");
            seed(locationRepository, "Tashkent, Uzbequistão",           "Ásia e Médio Oriente");
            seed(locationRepository, "Pequim, China",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Xangai, China",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Cantão, China",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Shenzhen, China",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Chengdu, China",                  "Ásia e Médio Oriente");
            seed(locationRepository, "Hong Kong",                       "Ásia e Médio Oriente");
            seed(locationRepository, "Taipé, Taiwan",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Tóquio, Japão",                   "Ásia e Médio Oriente");
            seed(locationRepository, "Osaka, Japão",                    "Ásia e Médio Oriente");
            seed(locationRepository, "Seul, Coreia do Sul",             "Ásia e Médio Oriente");
            seed(locationRepository, "Busan, Coreia do Sul",            "Ásia e Médio Oriente");
            seed(locationRepository, "Singapura, Singapura",            "Ásia e Médio Oriente");
            seed(locationRepository, "Kuala Lumpur, Malásia",           "Ásia e Médio Oriente");
            seed(locationRepository, "Jacarta, Indonésia",              "Ásia e Médio Oriente");
            seed(locationRepository, "Bali, Indonésia",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Manila, Filipinas",               "Ásia e Médio Oriente");
            seed(locationRepository, "Banguecoque, Tailândia",          "Ásia e Médio Oriente");
            seed(locationRepository, "Ho Chi Minh, Vietname",           "Ásia e Médio Oriente");
            seed(locationRepository, "Hanói, Vietname",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Phnom Penh, Camboja",             "Ásia e Médio Oriente");
            seed(locationRepository, "Vientiane, Laos",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Rangum, Myanmar",                 "Ásia e Médio Oriente");
            seed(locationRepository, "Ulan Bator, Mongólia",            "Ásia e Médio Oriente");

            // ── Oceânia ──────────────────────────────────────────────────────
            seed(locationRepository, "Sydney, Austrália",               "Oceânia");
            seed(locationRepository, "Melbourne, Austrália",            "Oceânia");
            seed(locationRepository, "Brisbane, Austrália",             "Oceânia");
            seed(locationRepository, "Perth, Austrália",                "Oceânia");
            seed(locationRepository, "Adelaide, Austrália",             "Oceânia");
            seed(locationRepository, "Auckland, Nova Zelândia",         "Oceânia");
            seed(locationRepository, "Wellington, Nova Zelândia",       "Oceânia");
            seed(locationRepository, "Christchurch, Nova Zelândia",     "Oceânia");
            seed(locationRepository, "Port Moresby, Papua Nova Guiné",  "Oceânia");
            seed(locationRepository, "Suva, Fiji",                      "Oceânia");
            seed(locationRepository, "Honolulu, EUA (Havai)",           "Oceânia");

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
