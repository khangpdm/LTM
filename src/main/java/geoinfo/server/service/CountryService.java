package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CountryService {
    private static final String ALL_COUNTRIES_URL = "https://restcountries.com/v3.1/all?fields=name,capital,altSpellings,cca2,cca3,latlng,population,currencies,languages,borders";
    private static final String FLAG_URL_TEMPLATE = "https://flagcdn.com/w320/%s.png";
    private static final String COUNTRY_ALIAS_RESOURCE = "/data/vietsub.csv";

    private static JSONArray countriesCache;
    private static volatile Map<String, String> countryAliasCache;

    public static String getCountryInfo(String input) {
        try {
            JSONObject country = getValidatedCountry(input);
            JSONObject nameObj = country.optJSONObject("name");
            String commonName = nameObj == null ? input : nameObj.optString("common", input);
            String cca2 = country.optString("cca2", "");

            JSONArray latlngArr = country.optJSONArray("latlng");
            String coordinates = latlngArr == null ? "Empty" : latlngArr.toString();
            String population = String.valueOf(country.optLong("population", 0));
            String currencies = buildCurrencies(country.optJSONObject("currencies"));
            String languages = buildLanguages(country.optJSONObject("languages"));
            String neighboringCountries = formatBorders(country.optJSONArray("borders"));
            String currentWeather = CityService.getWeatherSummary(getCapitalOrCountryName(country, commonName));

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
            response.put("flagUrl", buildFlagUrl(cca2));
            response.put("moreInfoRequest", "country-more:" + commonName);
            response.put("moreInfoLabel", "More Information");
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Error getting country data: " + e.getMessage());
        }
    }

    public static String getCountryMoreInfo(String input) {
        try {
            JSONObject country = getValidatedCountry(input);
            JSONObject nameObj = country.optJSONObject("name");
            String commonName = nameObj == null ? input : nameObj.optString("common", input);
            String cca2 = country.optString("cca2", "");

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "countryMoreInfo");
            response.put("name", commonName);
            response.put("news", extractItems(NewsService.getNewsInfo(commonName)));
            response.put("attractions", extractItems(AttractionService.getAttractionInfo(cca2)));
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Error getting country data: " + e.getMessage());
        }
    }

    public static String reloadCountries() {
        try {
            countriesCache = null;
            countryAliasCache = null;
            ensureCountriesLoaded();
            return new JSONObject()
                    .put("status", "success")
                    .put("type", "country")
                    .put("message", "The data has been reloaded.")
                    .toString(2);
        } catch (Exception e) {
            return new JSONObject()
                    .put("status", "error")
                    .put("type", "country")
                    .put("message", "Error reloading country data: " + e.getMessage())
                    .toString(2);
        }
    }

    private static JSONObject getValidatedCountry(String input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("No data");
        }

        input = input.replaceAll("\\s+", " ").trim();

        String cleanInput = normalizeName(input);
        if (cleanInput.length() < 2 && !cleanInput.equals("y")) { // ngoại lệ khi tìm nước "ý"
            throw new IllegalArgumentException("My country name is too short.");
        }
        if (input.isEmpty()) {
            throw new IllegalArgumentException("No data.");
        }
        if (input.length() > 100) {
            throw new IllegalArgumentException("My country name is too long.");
        }
        if (!ValidationUtils.isValidLocationName(input)) {
            throw new IllegalArgumentException("My  country name is invalid characters.");
        }

        String originalInput = input;
        input = resolveCountryAlias(input);
        boolean aliasResolved = !originalInput.equals(input);
        boolean allowShortCodeAltMatch = !aliasResolved && isUppercaseCountryCodeQuery(originalInput);
        ensureCountriesLoaded();
        JSONObject country = findCountry(input, allowShortCodeAltMatch);
        if (country == null) {
            throw new IllegalArgumentException("Not found country.");
        }
        return country;
    }


    private static synchronized JSONObject findCountry(String keyword, boolean allowShortCodeAltMatch) {
        String cleanKeyword = normalizeName(keyword);
        if (cleanKeyword.isEmpty()) return null;

        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.getJSONObject(i);
            JSONObject nameObj = country.optJSONObject("name");

            // 1. Kiểm tra Common Name & Official Name
            if (normalizeName(nameObj.optString("common")).equals(cleanKeyword) ||
                    normalizeName(nameObj.optString("official")).equals(cleanKeyword)) {
                return country;
            }

            // 2. Kiểm tra Alt Spellings (Tên thay thế, mã code)
            JSONArray altSpellings = country.optJSONArray("altSpellings");
            if (altSpellings != null) {
                for (int j = 0; j < altSpellings.length(); j++) {
                    String alt = altSpellings.getString(j);
                    if (!allowShortCodeAltMatch && alt.length() <= 3) continue;
                    if (normalizeName(alt).equals(cleanKeyword)) return country;
                }
            }
        }
        return null; // Nếu không khớp chính xác, có thể thêm logic partialMatch ở đây nếu muốn
    }

    private static synchronized void ensureCountriesLoaded() throws Exception {
        if (countriesCache != null) {
            return;
        }

        Document doc = ApiConnector.get(ALL_COUNTRIES_URL);
        if (doc == null) {
            throw new IllegalStateException("Unable to connect country API: " + ALL_COUNTRIES_URL);
        }
        String body = doc.text();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Data is empty or invalid.");
        }
        countriesCache = new JSONArray(body);
    }

    private static String buildCurrencies(JSONObject currencies) {
        if (currencies == null || currencies.isEmpty()) {
            return "Empty";
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

        return result.isEmpty() ? "Empty" : result.toString();
    }

    private static String buildLanguages(JSONObject languages) {
        if (languages == null || languages.isEmpty()) {
            return "Empty";
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

        return result.isEmpty() ? "Empty" : result.toString();
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
            return "Empty";
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

        return result.isEmpty() ? "Empty" : result.toString();
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

    private static String buildFlagUrl(String cca2) {
        if (cca2 == null || cca2.isBlank()) {
            return "";
        }
        return FLAG_URL_TEMPLATE.formatted(cca2.trim().toLowerCase());
    }

    private static String normalizeName(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace("đ", "d").replace("Đ", "D");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]", "") // Xóa sạch ký tự đặc biệt và CẢ KHOẢNG TRẮNG
                .trim();
    }

    private static boolean isUppercaseCountryCodeQuery(String input) {
        if (input == null) {
            return false;
        }
        String compact = input.replaceAll("\\s+", "");
        return compact.matches("[A-Z]{2,3}");
    }

    private static boolean isShortCountryCodeToken(String value) {
        if (value == null) {
            return false;
        }
        return value.trim().matches("[A-Z]{2,3}");
    }

    private static String resolveCountryAlias(String keyword) {
        ensureCountryAliasesLoaded();
        if (countryAliasCache == null || countryAliasCache.isEmpty()) {
            return keyword;
        }

        String cleanKeyword = normalizeName(keyword);

        // Truy vấn trực tiếp từ Cache
        String mapped = countryAliasCache.get(cleanKeyword);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return keyword;
    }

    private static synchronized void ensureCountryAliasesLoaded() {
        if (countryAliasCache != null) {
            return;
        }

        Map<String, String> aliases = new HashMap<>();
        try (InputStream stream = CountryService.class.getResourceAsStream(COUNTRY_ALIAS_RESOURCE)) {
            if (stream == null) {
                System.err.println("Country alias file not found: " + COUNTRY_ALIAS_RESOURCE);
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        addCountryAlias(aliases, line);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Unable to load country aliases: " + e.getMessage());
        }

        addDefaultCountryAliases(aliases);
        countryAliasCache = aliases.isEmpty() ? Map.of() : Map.copyOf(aliases);
    }

    private static void addAliasMapping(Map<String, String> aliases, String alias, String canonicalName) {
        if (alias == null || canonicalName == null) {
            return;
        }

        String normalizedCanonicalName = canonicalName.trim();
        if (alias.isBlank() || normalizedCanonicalName.isBlank()) {
            return;
        }

        String cleanAlias = normalizeName(alias);
        aliases.putIfAbsent(cleanAlias, normalizedCanonicalName);
    }

    private static void addDefaultCountryAliases(Map<String, String> aliases) {
        addAliasMapping(aliases, "usa", "United States");
    }

    private static void addCountryAlias(Map<String, String> aliases, String rawLine) {
        if (rawLine == null) return;
        // Xóa ký tự BOM và khoảng trắng thừa
        String line = rawLine.replace("\uFEFF", "").trim();
        if (line.isEmpty() || line.startsWith("#")) return;

        if (line.startsWith("\"") && line.endsWith("\"")) {
            line = line.substring(1, line.length() - 1);
        }

        String[] parts = line.split(",");
        if (parts.length < 2) return;

        String alias = stripWrappingQuotes(parts[0]);
        String canonicalName = stripWrappingQuotes(parts[1]);

        addAliasMapping(aliases, alias, canonicalName);
    }

    private static String stripWrappingQuotes(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            return normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
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
}
