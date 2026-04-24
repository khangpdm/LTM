package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CountryService {
    private static final String ALL_COUNTRIES_URL = "https://restcountries.com/v3.1/all?fields=name,capital,altSpellings,cca2,cca3,latlng,population,currencies,languages,borders";
    private static final String FLAG_URL_TEMPLATE = "https://flagcdn.com/w320/%s.png";
    private static final String COUNTRY_ALIAS_RESOURCE = "/data/vietsub.csv";

    private static JSONArray countriesCache;
    private static volatile Map<String, String> countryAliasCache;

    // 1. LẤY THÔNG TIN CƠ BẢN CỦA MỘT QUỐC GIA DỰA TRÊN TÊN ĐẦU VÀO (TÊN TIẾNG ANH, TÊN TIẾNG VIỆT, MÃ CODE)
    public static String getCountryInfo(String input) {
        try {
            // Note: Lấy đối tượng JSON quốc gia
            JSONObject country = getValidatedCountry(input);
            // Note: Trích xuất các thông tin: commonName, cca2, coordinates, population, ...
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
            // Note: Xây dựng JSON response với status "success", type "country"
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

    // 2. LẤY THÔNG TIN BỔ SUNG VỀ QUỐC GIA (TIN TỨC VÀ ĐIỂM THAM QUAN)
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

    // 3. RELOAD LẠI DỮ LIỆU QUỐC GIA TỪ API, XÓA CACHE CŨ
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

    // 1.1. Xác thực và tìm kiếm quốc gia dựa trên input, hỗ trợ alias tiếng Việt và mã code.
    private static JSONObject getValidatedCountry(String input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("No data");
        }

        input = ValidationUtils.validateLocationInput(input, true);

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

    // 1.1.1. Kiểm tra xem input có phải là mã quốc gia viết hoa gồm 2-3 chữ cái không
    private static boolean isUppercaseCountryCodeQuery(String input) {
        if (input == null) return false;
        String compact = input.replaceAll("\\s+", "");
        return compact.matches("[A-Z]{2,3}");
    }
    // 1.1.2. Chuyển đổi alias (tên tiếng Việt hoặc tên thông dụng) thành tên quốc gia chuẩn
    private static String resolveCountryAlias(String keyword) {
        ensureCountryAliasesLoaded();
        if (countryAliasCache == null || countryAliasCache.isEmpty()) {
            return keyword;
        }

        String cleanKeyword = ValidationUtils.normalizeName(keyword);

        // Note: Truy vấn trực tiếp từ Cache
        String mapped = countryAliasCache.get(cleanKeyword);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return keyword;
    }
    // 1.1.3.  Đảm bảo cache countriesCache đã được tải từ API RestCountries
    private static synchronized void ensureCountriesLoaded() throws Exception {
        if (countriesCache != null) return;

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
    // 1.1.4. Tìm kiếm quốc gia trong cache dựa trên keyword đã chuẩn hóa
    private static synchronized JSONObject findCountry(String keyword, boolean allowShortCodeAltMatch) {
        String cleanKeyword = ValidationUtils.normalizeName(keyword);
        if (cleanKeyword.isEmpty())
            return null;

        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.getJSONObject(i);
            JSONObject nameObj = country.optJSONObject("name");
            // Note: Kiểm tra Common Name & Official Name
            if (ValidationUtils.normalizeName(nameObj.optString("common")).equals(cleanKeyword) || ValidationUtils.normalizeName(nameObj.optString("official")).equals(cleanKeyword))
                return country;
            // Note: Kiểm tra Alt Spellings (Tên thay thế, mã code)
            JSONArray altSpellings = country.optJSONArray("altSpellings");
            if (altSpellings != null) {
                for (int j = 0; j < altSpellings.length(); j++) {
                    String alt = altSpellings.getString(j);
                    if (!allowShortCodeAltMatch && alt.length() <= 3) continue;
                    if (ValidationUtils.normalizeName(alt).equals(cleanKeyword)) return country;
                }
            }
        }
        return null;
    }

    // 1.2. Xây dựng chuỗi mô tả các loại tiền tệ từ JSON currencies
    private static String buildCurrencies(JSONObject currencies) {
        if (currencies == null || currencies.isEmpty())
            return "Empty";

        StringBuilder result = new StringBuilder();
        for (String code : currencies.keySet()) {
            JSONObject currency = currencies.optJSONObject(code);
            if (currency == null)
                continue;

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

    // 1.3.  Xây dựng chuỗi danh sách các ngôn ngữ từ JSON languages.
    private static String buildLanguages(JSONObject languages) {
        if (languages == null || languages.isEmpty())
            return "Empty";

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

    // 1.4. Lấy tên thủ đô của quốc gia, nếu không có thì trả về fallbackName (dùng để lấy thời tiết)
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

    // 1.5. Định dạng danh sách các nước láng giềng từ mã border (cca3) thành tên quốc gia
    private static String formatBorders(JSONArray borders) {
        if (borders == null || borders.isEmpty())
            return "Empty";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < borders.length(); i++) {
            String borderCode = borders.optString(i, "");
            if (borderCode.isBlank())
                continue;

            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(findCountryNameByCca3(borderCode));
        }

        return result.isEmpty() ? "Empty" : result.toString();
    }
    // 1.5.1. Tìm tên quốc gia dựa trên mã cca3 (3 chữ cái)
    private static String findCountryNameByCca3(String cca3) {
        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.optJSONObject(i);
            if (country == null)
                continue;

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

    // 1.6. Tạo URL ảnh flag từ mã cca2 (2 chữ cái)
    private static String buildFlagUrl(String cca2) {
        if (cca2 == null || cca2.isBlank()) return "";
        return FLAG_URL_TEMPLATE.formatted(cca2.trim().toLowerCase());
    }

    // 1.7. Tạo JSON response cho trường hợp lỗi với status "error"
    private static String createErrorResponse(String message) {
        return new JSONObject()
                .put("status", "error")
                .put("type", "country")
                .put("message", message)
                .toString(2);
    }

    // 2.1. Trích xuất mảng "items" từ JSON text trả về từ service khác
    private static JSONArray extractItems(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            return json.optJSONArray("items") == null ? new JSONArray() : json.optJSONArray("items");
        } catch (Exception e) {
            return new JSONArray();
        }
    }


    // 1.1.3.1. Đảm bảo cache alias quốc gia đã được tải từ file CSV
    private static synchronized void ensureCountryAliasesLoaded() {
        if (countryAliasCache != null) return;

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
    // 1.1.3.1.1. Parse một dòng từ file CSV và thêm mapping alias ra tên chuẩn vào map
    private static void addCountryAlias(Map<String, String> aliases, String rawLine) {
        if (rawLine == null) return;
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
    // 1.1.3.1.2. Thêm các alias mặc định cho usa
    private static void addDefaultCountryAliases(Map<String, String> aliases) {
        addAliasMapping(aliases, "usa", "United States");
    }
    // 1.1.3.1.3. Parse một dòng từ file CSV và thêm mapping alias → tên chuẩn vào map.
    private static void addAliasMapping(Map<String, String> aliases, String alias, String canonicalName) {
        if (alias == null || canonicalName == null) {return;}

        String normalizedCanonicalName = canonicalName.trim();
        if (alias.isBlank() || normalizedCanonicalName.isBlank()) {return;}

        String cleanAlias = ValidationUtils.normalizeName(alias);
        aliases.putIfAbsent(cleanAlias, normalizedCanonicalName);
    }
    // 1.1.3.1.4. Loại bỏ dấu ngoặc kép ở đầu và cuối chuỗi nếu có
    private static String stripWrappingQuotes(String value) {
        if (value == null) {return "";}

        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            return normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }
}