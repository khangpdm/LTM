package geoinfo.server.service;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountryServiceTest {

    @BeforeEach
    void setUp() throws Exception {
        setCountriesCache(new JSONArray("""
                [
                  {
                    "cca3": "CHN",
                    "name": {
                      "common": "China",
                      "official": "People's Republic of China"
                    },
                    "altSpellings": ["CN", "PRC"],
                    "latlng": [35, 105],
                    "population": 1408280000,
                    "currencies": {
                      "CNY": {
                        "name": "Chinese yuan",
                        "symbol": "¥"
                      }
                    },
                    "languages": {
                      "zho": "Chinese"
                    },
                    "flag": "CN",
                    "borders": ["VNM", "LAO"]
                  },
                  {
                    "cca3": "VNM",
                    "name": {
                      "common": "Việt Nam",
                      "official": "Socialist Republic of Vietnam"
                    },
                    "altSpellings": ["VN", "Viet Nam"],
                    "latlng": [16.16666666, 107.83333333],
                    "population": 101343800,
                    "currencies": {
                      "VND": {
                        "name": "Vietnamese dong",
                        "symbol": "d"
                      }
                    },
                    "languages": {
                      "vie": "Vietnamese"
                    },
                    "flag": "VN",
                    "borders": ["KHM", "CHN", "LAO"]
                  },
                  {
                    "cca3": "LAO",
                    "name": {
                      "common": "Laos",
                      "official": "Lao People's Democratic Republic"
                    },
                    "altSpellings": ["LA", "Lao"],
                    "latlng": [18, 105],
                    "population": 7500000,
                    "currencies": {
                      "LAK": {
                        "name": "Lao kip",
                        "symbol": "K"
                      }
                    },
                    "languages": {
                      "lao": "Lao"
                    },
                    "flag": "LA",
                    "borders": ["CHN", "VNM"]
                  },
                  {
                    "cca3": "KHM",
                    "name": {
                      "common": "Cambodia",
                      "official": "Kingdom of Cambodia"
                    },
                    "altSpellings": ["KH"],
                    "latlng": [13, 105],
                    "population": 17000000,
                    "currencies": {
                      "KHR": {
                        "name": "Cambodian riel",
                        "symbol": "r"
                      }
                    },
                    "languages": {
                      "khm": "Khmer"
                    },
                    "flag": "KH",
                    "borders": ["VNM", "LAO"]
                  },
                  {
                    "cca3": "JPN",
                    "name": {
                      "common": "Japan",
                      "official": "Japan"
                    },
                    "altSpellings": ["JP", "Nippon", "Nihon"],
                    "latlng": [36, 138],
                    "population": 123210000,
                    "currencies": {
                      "JPY": {
                        "name": "Japanese yen",
                        "symbol": "¥"
                      }
                    },
                    "languages": {
                      "jpn": "Japanese"
                    },
                    "flag": "JP",
                    "borders": []
                  },
                  {
                    "cca3": "USA",
                    "name": {
                      "common": "United States",
                      "official": "United States of America"
                    },
                    "altSpellings": ["US", "USA", "United States"],
                    "latlng": [38, -97],
                    "population": 331000000,
                    "currencies": {
                      "USD": {
                        "name": "United States dollar",
                        "symbol": "$"
                      }
                    },
                    "languages": {
                      "eng": "English"
                    },
                    "flag": "US",
                    "borders": []
                  },
                  {
                    "cca3": "CIV",
                    "name": {
                      "common": "Côte d'Ivoire",
                      "official": "Republic of Côte d'Ivoire"
                    },
                    "altSpellings": ["CI", "Ivory Coast", "Cote d'Ivoire"],
                    "latlng": [8, -5],
                    "population": 29000000,
                    "currencies": {
                      "XOF": {
                        "name": "West African CFA franc",
                        "symbol": "CFA"
                      }
                    },
                    "languages": {
                      "fra": "French"
                    },
                    "flag": "CI",
                    "borders": []
                  }
                ]
                """));
    }

    @AfterEach
    void tearDown() throws Exception {
        setCountriesCache(null);
    }

    @Test
    void shouldFindByExactCommonName() {
        String result = CountryService.getCountryInfo("china");

        assertTrue(result.contains("Chinese yuan"));
        assertTrue(result.contains("[35,105]"));
    }

    @Test
    void shouldIgnoreCaseWhenFindingCountry() {
        String result = CountryService.getCountryInfo("VIỆT NAM");

        assertTrue(result.contains("Vietnamese dong"));
        assertTrue(result.contains("[16.16666666,107.83333333]"));
    }

    @Test
    void shouldFindByOfficialName() {
        String result = CountryService.getCountryInfo("People's Republic of China");

        assertTrue(result.contains("Chinese yuan"));
        assertTrue(result.contains("Chinese"));
    }

    @Test
    void shouldFindByAlternativeSpelling() {
        String result = CountryService.getCountryInfo("PRC");

        assertTrue(result.contains("Chinese yuan"));
        assertTrue(result.contains("Quốc kỳ"));
    }

    @Test
    void shouldFindByCompactCommonName() {
        String result = CountryService.getCountryInfo("viet nam");

        assertTrue(result.contains("Vietnamese dong"));
        assertTrue(result.contains("Vietnamese"));
    }

    @Test
    void shouldFindByCompactOfficialName() {
        String result = CountryService.getCountryInfo("unitedstatesofamerica");

        assertTrue(result.contains("United States dollar"));
        assertTrue(result.contains("English"));
    }

    @Test
    void shouldFindByCompactAltSpelling() {
        String result = CountryService.getCountryInfo("unitedstates");

        assertTrue(result.contains("United States dollar"));
        assertTrue(result.contains("Quốc kỳ"));
    }

    @Test
    void shouldFindByAccentInsensitiveCommonName() {
        String result = CountryService.getCountryInfo("viet nam");

        assertTrue(result.contains("Vietnamese dong"));
        assertTrue(result.contains("Việt Nam") || result.contains("[16.16666666,107.83333333]"));
    }

    @Test
    void shouldFindByAccentInsensitiveOfficialName() {
        String result = CountryService.getCountryInfo("republic of cote d'ivoire");

        assertTrue(result.contains("West African CFA franc"));
        assertTrue(result.contains("French"));
    }

    @Test
    void shouldFindByCompactAccentInsensitiveAltSpelling() {
        String result = CountryService.getCountryInfo("cotedivoire");

        assertTrue(result.contains("West African CFA franc"));
        assertTrue(result.contains("Quốc kỳ"));
    }

    @Test
    void shouldFallbackToPartialMatch() {
        String result = CountryService.getCountryInfo("viet");

        assertTrue(result.contains("Vietnamese dong"));
        assertTrue(result.contains("Vietnamese"));
    }

    @Test
    void shouldReturnErrorForBlankInput() {
        String result = CountryService.getCountryInfo("   ");

        assertTrue(result.contains("dữ liệu đầu vào rỗng"));
    }

    @Test
    void shouldReturnErrorWhenCountryIsNotFound() {
        String result = CountryService.getCountryInfo("atlantis");

        assertTrue(result.contains("không tìm thấy quốc gia phù hợp"));
    }

    @Test
    void shouldConvertBorderCodesToCountryNames() {
        String result = CountryService.getCountryInfo("việt nam");

        assertTrue(result.contains("Cambodia"));
        assertTrue(result.contains("China"));
        assertTrue(result.contains("Laos"));
        assertFalse(result.contains("KHM"));
        assertFalse(result.contains("CHN"));
        assertFalse(result.contains("LAO"));
    }

    private void setCountriesCache(JSONArray value) throws Exception {
        Field field = CountryService.class.getDeclaredField("countriesCache");
        field.setAccessible(true);
        field.set(null, value);
    }
}
