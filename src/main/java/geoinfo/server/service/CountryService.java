package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.text.Normalizer;
import java.util.Scanner;

public class CountryService {
    private static final String FLAG_URL_TEMPLATE = "https://flagcdn.com/w320/%s.png";
    private static final String ALL_COUNTRIES_URL =
            "https://restcountries.com/v3.1/all?fields=name,capital,altSpellings,cca2,cca3,latlng,population,currencies,languages,borders";
    private static JSONArray countriesCache;

    public static String getCountryInfo(String input) {
        try {
            return getCountryDetails(input).summary();
        } catch (Exception e) {
            return "Loi khi lay du lieu quoc gia: " + e.getMessage();
        }
    }

    public static String getCountryNews(String input) {
        try {
            CountryDetails details = getCountryDetails(input);
            return "===============================================\n"
                    + "TIN TUC: " + details.commonName() + "\n"
                    + details.news();
        } catch (Exception e) {
            return "Loi khi lay tin tuc quoc gia: " + e.getMessage();
        }
    }

    public static CountryDetails getCountryDetails(String input) throws Exception {
        String validatedInput = validateInput(input);
        ensureCountriesLoaded();

        JSONObject json = findCountry(validatedInput);
        if (json == null) {
            throw new IllegalArgumentException("khong tim thay quoc gia phu hop.");
        }

        JSONObject nameObj = json.optJSONObject("name");
        String commonName = nameObj == null ? validatedInput : nameObj.optString("common", validatedInput);
        String cca2 = json.optString("cca2", "");

        JSONArray latlngArr = json.optJSONArray("latlng");
        String latlng = latlngArr == null ? "Khong co" : latlngArr.toString();
        String population = String.valueOf(json.optLong("population", 0));
        String currencies = buildCurrencies(json.optJSONObject("currencies"));
        String languages = buildLanguages(json.optJSONObject("languages"));
        String borders = formatBorders(json.optJSONArray("borders"));
        String weather = CityService.getWeatherSummary(getCapitalOrCountryName(json, commonName));
        String attractions = AttractionService.getAttractionInfo(cca2);
        String news = NewsService.getNewsInfo(commonName);
        String flagUrl = buildFlagUrl(cca2);

        String summary = "===============================================\n"
                + "QUOC GIA: " + commonName + "\n"
                + "Toa do: " + latlng + "\n"
                + "Dan so: " + population + "\n"
                + "Don vi tien te: " + currencies + "\n"
                + "Ngon ngu: " + languages + "\n"
                + "Quoc gia lang gieng: " + borders + "\n"
                + "Thoi tiet hien tai: " + weather + "\n"
                + "===============================================\n"
                + attractions;

        return new CountryDetails(commonName, flagUrl, summary, news);
    }

    public static String reloadCountries() {
        try {
            countriesCache = null;
            ensureCountriesLoaded();
            return "Da tai lai du lieu quoc gia.";
        } catch (Exception e) {
            return "Loi khi tai lai du lieu quoc gia: " + e.getMessage();
        }
    }

    private static String validateInput(String input) {
        if (input == null) {
            throw new IllegalArgumentException("du lieu dau vao rong.");
        }

        input = input.replaceAll("\\s+", " ").trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("du lieu dau vao rong.");
        }
        if (input.length() < 2) {
            throw new IllegalArgumentException("ten quoc gia qua ngan.");
        }
        if (input.length() > 100) {
            throw new IllegalArgumentException("ten quoc gia qua dai.");
        }
        if (!ValidationUtils.isValidLocationName(input)) {
            throw new IllegalArgumentException("ten quoc gia chua ky tu khong hop le.");
        }
        return input;
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
            if (accentInsensitiveCommonMatch == null && accentInsensitiveKeyword.equals(accentInsensitiveCommonName)) {
                accentInsensitiveCommonMatch = country;
            }
            if (accentInsensitiveOfficialMatch == null && accentInsensitiveKeyword.equals(accentInsensitiveOfficialName)) {
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
                    && (commonName.startsWith(normalizedKeyword) || officialName.startsWith(normalizedKeyword))) {
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
            throw new IllegalStateException("khong the ket noi API quoc gia: " + ALL_COUNTRIES_URL);
        }
        String body = doc.text();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("du lieu quoc gia trong hoac khong hop le");
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

    private static String buildFlagUrl(String cca2) {
        if (cca2 == null || cca2.isBlank()) {
            return "";
        }
        return FLAG_URL_TEMPLATE.formatted(cca2.trim().toLowerCase());
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

    public record CountryDetails(String commonName, String flagUrl, String summary, String news) {
    }
}
