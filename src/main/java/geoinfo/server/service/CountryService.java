package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.text.Normalizer;
import java.util.Scanner;

public class CountryService {
    private static final String ALL_COUNTRIES_URL =
            "https://restcountries.com/v3.1/all?fields=name,capital,altSpellings,cca2,cca3,latlng,population,currencies,languages,borders";
    private static JSONArray countriesCache;

    public static String getCountryInfo(String input) {
        try {
            if (input == null) {
                return createErrorResponse("Loi khi lay du lieu quoc gia: du lieu dau vao rong.");
            }

            input = input.replaceAll("\\s+", " ").trim();

            if (input.isEmpty()) {
                return createErrorResponse("Loi khi lay du lieu quoc gia: du lieu dau vao rong.");
            }

            if (input.length() < 2) {
                return createErrorResponse("Loi khi lay du lieu quoc gia: ten quoc gia qua ngan.");
            }

            if (input.length() > 100) {
                return createErrorResponse("Loi khi lay du lieu quoc gia: ten quoc gia qua dai.");
            }

            if (!ValidationUtils.isValidLocationName(input)) {
                return createErrorResponse("Loi khi lay du lieu quoc gia: ten quoc gia chua ky tu khong hop le.");
            }

            ensureCountriesLoaded();

            JSONObject json = findCountry(input);
            if (json == null) {
                return createErrorResponse("Loi khi lay du lieu quoc gia: khong tim thay quoc gia phu hop.");
            }

            JSONObject nameObj = json.optJSONObject("name");
            String commonName = nameObj == null ? input : nameObj.optString("common", input);
            String cca2 = json.optString("cca2", "");

            JSONArray latlngArr = json.optJSONArray("latlng");
            String coordinates = latlngArr == null ? "Khong co" : latlngArr.toString();
            String population = String.valueOf(json.optLong("population", 0));
            String currencies = buildCurrencies(json.optJSONObject("currencies"));
            String languages = buildLanguages(json.optJSONObject("languages"));
            String neighboringCountries = formatBorders(json.optJSONArray("borders"));
            String currentWeather = CityService.getWeatherSummary(getCapitalOrCountryName(json, commonName));
            String newsJson = NewsService.getNewsInfo(commonName);
            String attractionsJson = AttractionService.getAttractionInfo(cca2);

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "country");
            response.put("name", commonName);
            response.put("coordinates", coordinates);
            response.put("population", population);
            response.put("currencies", currencies);
            response.put("languages", languages);
            response.put("neighboringCountries", neighboringCountries);
            response.put("currentWeather", currentWeather);
            response.put("news", extractItems(newsJson));
            response.put("attractions", extractItems(attractionsJson));
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Loi khi lay du lieu quoc gia: " + e.getMessage());
        }
    }

    public static String reloadCountries() {
        try {
            countriesCache = null;
            ensureCountriesLoaded();
            return new JSONObject()
                    .put("status", "success")
                    .put("type", "country")
                    .put("message", "Da tai lai du lieu quoc gia.")
                    .toString(2);
        } catch (Exception e) {
            return new JSONObject()
                    .put("status", "error")
                    .put("type", "country")
                    .put("message", "Loi khi tai lai du lieu quoc gia: " + e.getMessage())
                    .toString(2);
        }
    }

    private static synchronized JSONObject findCountry(String keyword) {
        String normalizedKeyword = normalize(keyword);
        String compactKeyword = normalizeCompact(keyword);
        String accentInsensitiveKeyword = normalizeAccentInsensitive(keyword);
        String compactAccentInsensitiveKeyword = normalizeCompactAccentInsensitive(keyword);

        JSONObject exactCommonMatch = null;
        JSONObject exactOfficialMatch = null;
        JSONObject exactAltSpellingMatch = null;
        JSONObject compactCommonMatch = null;
        JSONObject compactOfficialMatch = null;
        JSONObject compactAltSpellingMatch = null;
        JSONObject accentInsensitiveCommonMatch = null;
        JSONObject accentInsensitiveOfficialMatch = null;
        JSONObject accentInsensitiveAltSpellingMatch = null;
        JSONObject compactAccentInsensitiveCommonMatch = null;
        JSONObject compactAccentInsensitiveOfficialMatch = null;
        JSONObject compactAccentInsensitiveAltSpellingMatch = null;
        JSONObject partialMatch = null;

        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.getJSONObject(i);
            if (country == null) {
                continue;
            }

            JSONObject name = country.optJSONObject("name");
            String commonName = name == null ? "" : normalize(name.optString("common"));
            String officialName = name == null ? "" : normalize(name.optString("official"));
            String compactCommonName = normalizeCompact(commonName);
            String compactOfficialName = normalizeCompact(officialName);
            String accentInsensitiveCommonName = normalizeAccentInsensitive(commonName);
            String accentInsensitiveOfficialName = normalizeAccentInsensitive(officialName);
            String compactAccentInsensitiveCommonName = normalizeCompactAccentInsensitive(commonName);
            String compactAccentInsensitiveOfficialName = normalizeCompactAccentInsensitive(officialName);

            if (normalizedKeyword.equals(commonName)) {
                exactCommonMatch = country;
                break;
            }

            if (exactOfficialMatch == null && normalizedKeyword.equals(officialName)) {
                exactOfficialMatch = country;
            }

            if (compactCommonMatch == null && compactKeyword.equals(compactCommonName)) {
                compactCommonMatch = country;
            }

            if (compactOfficialMatch == null && compactKeyword.equals(compactOfficialName)) {
                compactOfficialMatch = country;
            }

            if (accentInsensitiveCommonMatch == null
                    && accentInsensitiveKeyword.equals(accentInsensitiveCommonName)) {
                accentInsensitiveCommonMatch = country;
            }

            if (accentInsensitiveOfficialMatch == null
                    && accentInsensitiveKeyword.equals(accentInsensitiveOfficialName)) {
                accentInsensitiveOfficialMatch = country;
            }

            if (compactAccentInsensitiveCommonMatch == null
                    && compactAccentInsensitiveKeyword.equals(compactAccentInsensitiveCommonName)) {
                compactAccentInsensitiveCommonMatch = country;
            }

            if (compactAccentInsensitiveOfficialMatch == null
                    && compactAccentInsensitiveKeyword.equals(compactAccentInsensitiveOfficialName)) {
                compactAccentInsensitiveOfficialMatch = country;
            }

            JSONArray altSpellings = country.optJSONArray("altSpellings");
            if (altSpellings != null) {
                for (int j = 0; j < altSpellings.length(); j++) {
                    String altSpelling = altSpellings.optString(j);
                    String normalizedAlt = normalize(altSpelling);
                    String compactAlt = normalizeCompact(altSpelling);
                    String accentInsensitiveAlt = normalizeAccentInsensitive(altSpelling);
                    String compactAccentInsensitiveAlt = normalizeCompactAccentInsensitive(altSpelling);

                    if (exactAltSpellingMatch == null && normalizedKeyword.equals(normalizedAlt)) {
                        exactAltSpellingMatch = country;
                    }

                    if (compactAltSpellingMatch == null && compactKeyword.equals(compactAlt)) {
                        compactAltSpellingMatch = country;
                    }

                    if (accentInsensitiveAltSpellingMatch == null
                            && accentInsensitiveKeyword.equals(accentInsensitiveAlt)) {
                        accentInsensitiveAltSpellingMatch = country;
                    }

                    if (compactAccentInsensitiveAltSpellingMatch == null
                            && compactAccentInsensitiveKeyword.equals(compactAccentInsensitiveAlt)) {
                        compactAccentInsensitiveAltSpellingMatch = country;
                    }
                }
            }

            if (partialMatch == null && normalizedKeyword.length() >= 4
                    && (commonName.startsWith(normalizedKeyword)
                    || officialName.startsWith(normalizedKeyword))) {
                partialMatch = country;
            }
        }

        if (exactCommonMatch != null) return exactCommonMatch;
        if (exactOfficialMatch != null) return exactOfficialMatch;
        if (exactAltSpellingMatch != null) return exactAltSpellingMatch;
        if (compactCommonMatch != null) return compactCommonMatch;
        if (compactOfficialMatch != null) return compactOfficialMatch;
        if (compactAltSpellingMatch != null) return compactAltSpellingMatch;
        if (accentInsensitiveCommonMatch != null) return accentInsensitiveCommonMatch;
        if (accentInsensitiveOfficialMatch != null) return accentInsensitiveOfficialMatch;
        if (accentInsensitiveAltSpellingMatch != null) return accentInsensitiveAltSpellingMatch;
        if (compactAccentInsensitiveCommonMatch != null) return compactAccentInsensitiveCommonMatch;
        if (compactAccentInsensitiveOfficialMatch != null) return compactAccentInsensitiveOfficialMatch;
        if (compactAccentInsensitiveAltSpellingMatch != null) return compactAccentInsensitiveAltSpellingMatch;
        return partialMatch;
    }

    private static synchronized void ensureCountriesLoaded() throws Exception {
        if (countriesCache != null) {
            return;
        }

        Document doc = ApiConnector.get(ALL_COUNTRIES_URL);
        if (doc == null) {
            throw new IllegalStateException("Khong the ket noi API quoc gia: " + ALL_COUNTRIES_URL);
        }
        String body = doc.text();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Du lieu quoc gia trong hoac khong hop le");
        }
        countriesCache = new JSONArray(body);
    }

    private static String buildCurrencies(JSONObject currencies) {
        if (currencies == null || currencies.isEmpty()) {
            return "Khong co";
        }

        StringBuilder result = new StringBuilder();
        for (String code : currencies.keySet()) {
            JSONObject currency = currencies.optJSONObject(code);
            if (currency == null) {
                continue;
            }

            String name = currency.optString("name", "");
            String symbol = currency.optString("symbol", "");
            if (!name.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(name);
                if (!symbol.isBlank()) {
                    result.append(" - ").append(symbol);
                }
            }
        }

        return result.isEmpty() ? "Khong co" : result.toString();
    }

    private static String buildLanguages(JSONObject languages) {
        if (languages == null || languages.isEmpty()) {
            return "Khong co";
        }

        StringBuilder result = new StringBuilder();
        for (String code : languages.keySet()) {
            String language = languages.optString(code, "");
            if (!language.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(language);
            }
        }

        return result.isEmpty() ? "Khong co" : result.toString();
    }

    private static String getCapitalOrCountryName(JSONObject country, String fallbackName) {
        JSONArray capitalArray = country.optJSONArray("capital");
        if (capitalArray != null && !capitalArray.isEmpty()) {
            String capital = capitalArray.optString(0, "");
            if (!capital.isBlank()) {
                return capital;
            }
        }
        return fallbackName;
    }

    private static String formatBorders(JSONArray borders) {
        if (borders == null || borders.isEmpty()) {
            return "Khong co";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < borders.length(); i++) {
            String borderCode = borders.optString(i, "");
            if (borderCode.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(findCountryNameByCca3(borderCode));
        }

        return result.isEmpty() ? "Khong co" : result.toString();
    }

    private static String findCountryNameByCca3(String cca3) {
        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.optJSONObject(i);
            if (country == null) {
                continue;
            }

            if (cca3.equalsIgnoreCase(country.optString("cca3", ""))) {
                JSONObject name = country.optJSONObject("name");
                if (name != null) {
                    String commonName = name.optString("common", "");
                    if (!commonName.isBlank()) {
                        return commonName;
                    }
                }
                return cca3;
            }
        }

        return cca3;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase()
                .replaceAll("[\\p{Punct}’‘`]", " ")
                .replaceAll("\\s+", " ");
    }

    private static String normalizeCompact(String value) {
        return normalize(value).replace(" ", "");
    }

    private static String normalizeAccentInsensitive(String value) {
        String normalized = normalize(value);
        String decomposed = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "");
    }

    private static String normalizeCompactAccentInsensitive(String value) {
        return normalizeAccentInsensitive(value).replace(" ", "");
    }

    private static String createErrorResponse(String message) {
        return new JSONObject()
                .put("status", "error")
                .put("type", "country")
                .put("message", message)
                .toString(2);
    }

    private static JSONArray extractItems(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            return json.optJSONArray("items") == null ? new JSONArray() : json.optJSONArray("items");
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = "";

        while (!input.equalsIgnoreCase("exit")) {
            System.out.print("Nhap ten quoc gia: ");
            input = scanner.nextLine();

            if (!input.equalsIgnoreCase("exit")) {
                System.out.println(getCountryInfo(input));
            }
        }

        scanner.close();
    }
}