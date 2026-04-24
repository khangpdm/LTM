package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CityService {
    private static final String WEATHER_API_KEY = "a7a0ef458a3642009a580805262003";
    private static final String API_NINJAS_KEY = "DgGav2PqKdwGSy3B82AaE4cdRFtnE7qNFcYF4Yj1";
    private static final String GEONAMES_USERNAME = "tinmi2005";

    private static final String WEATHER_CURRENT_API_URL = "https://api.weatherapi.com/v1/current.json";
    private static final String API_NINJAS_CITY_API_URL = "https://api.api-ninjas.com/v1/city";
    private static final String GEONAMES_SEARCH_API_URL = "http://api.geonames.org/searchJSON";

    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newFixedThreadPool(4);

    // 1. LẤY THÔNG TIN CƠ BẢN CỦA MỘT THÀNH PHỐ (THỜI TIẾT, VỊ TRÍ, DÂN SỐ) DỰA TRÊN TÊN ĐẦU VÀO
    public static String getCityInfo(String input) {
        try {
            String sanitized = ValidationUtils.validateLocationInput(input);
            String url = WEATHER_CURRENT_API_URL
                    + "?key=" + WEATHER_API_KEY
                    + "&q=" + URLEncoder.encode(sanitized.trim(), StandardCharsets.UTF_8)
                    + "&aqi=no";
            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());

            JSONObject location = json.optJSONObject("location");
            if (location == null) {
                return createErrorResponse("Failed to load city data: location is invalid.");
            }

            String name = location.optString("name");
            if (!isReasonablySameCity(sanitized, name)) {
                return createErrorResponse("Failed to load city data: not found city.");
            }

            String country = location.optString("country");
            double latitude = location.optDouble("lat");
            double longitude = location.optDouble("lon");
            String localTime = location.optString("localtime");

            JSONObject current = json.optJSONObject("current");
            if (current == null) {
                return createErrorResponse("Failed to load city data: missing weather data.");
            }

            double temperatureCelsius = current.optDouble("temp_c");
            JSONObject conditionJson = current.optJSONObject("condition");
            String weatherCondition = conditionJson == null ? "" : conditionJson.optString("text");
            int humidity = current.optInt("humidity");
            double windKph = current.optDouble("wind_kph");

            // === GỌI POPULATION SONG SONG ===
            String finalName = name;
            String finalCountry = country;
            double finalLat = latitude;
            double finalLon = longitude;

            CompletableFuture<String> populationF = CompletableFuture.supplyAsync(
                    () -> getCityPopulation(finalName, finalCountry, finalLat, finalLon),
                    BACKGROUND_EXECUTOR
            ).exceptionally(e -> "no data");

            String population = populationF.join();
            
            String currentWeather = temperatureCelsius + " °C, " + weatherCondition + ", humidity: " + humidity + "%, wind: " + windKph + " km/h";

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "city");
            response.put("name", name);
            response.put("country", country);
            response.put("population", population);
            response.put("coordinates", new JSONArray().put(longitude).put(latitude));
            response.put("localTime", localTime);
            response.put("currentWeather", currentWeather);
            response.put("moreInfoRequest", "city-more:" + name);
            response.put("moreInfoLabel", "More Information");
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Failed to load city data: " + e.getMessage());
        }
    }

    // 2. LẤY THÔNG TIN BỔ SUNG VỀ THÀNH PHỐ (TIN TỨC VÀ KHÁCH SẠN)
    public static String getCityMoreInfo(String input) {
        try {
            String sanitized = ValidationUtils.validateLocationInput(input);
            String url = WEATHER_CURRENT_API_URL
                    + "?key=" + WEATHER_API_KEY
                    + "&q=" + URLEncoder.encode(sanitized.trim(), StandardCharsets.UTF_8)
                    + "&aqi=no";

            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());
            JSONObject location = json.optJSONObject("location");
            if (location == null) {
                return createErrorResponse("Failed to load more city data: location is invalid.");
            }

            String name = location.optString("name");
            if (!isReasonablySameCity(sanitized, name)) {
                return createErrorResponse("Failed to load more city data.");
            }

            // === GỌI SONG SONG ===
            CompletableFuture<JSONArray> newsF = CompletableFuture.supplyAsync(
                    () -> extractItems(NewsService.getNewsInfo(name)),
                    BACKGROUND_EXECUTOR
            ).exceptionally(e -> new JSONArray());

            CompletableFuture<JSONArray> hotelsF = CompletableFuture.supplyAsync(
                    () -> extractItems(HotelService.getHotelsByCity(name)),
                    BACKGROUND_EXECUTOR
            ).exceptionally(e -> new JSONArray());

            // Chờ cả 2 hoàn thành
            CompletableFuture.allOf(newsF, hotelsF).join();

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "cityMoreInfo");
            response.put("name", name);
            response.put("news", newsF.join());
            response.put("hotels", hotelsF.join());
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Failed to load more city data: " + e.getMessage());
        }
    }

    // 3. LẤY TÓM TẮT THỜI TIẾT CHO MỘT ĐỊA ĐIỂM (THÀNH PHỐ HOẶC THỦ ĐÔ)
    public static String getWeatherSummary(String query) {
        if (ValidationUtils.isEmpty(query)) {
            return "no weather data.";
        }

        query = ValidationUtils.sanitizeInput(query);
        if (ValidationUtils.isEmpty(query)) {
            return "no weather data.";
        }

        String url = WEATHER_CURRENT_API_URL
                + "?key=" + WEATHER_API_KEY
                + "&q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                + "&aqi=no";
        try {
            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());

            JSONObject location = json.getJSONObject("location");
            JSONObject current = json.getJSONObject("current");

            String placeName = location.optString("name", query);
            double tempC = current.optDouble("temp_c", 0);
            String condition = current.optJSONObject("condition") == null
                    ? "no data"
                    : current.getJSONObject("condition").optString("text", "no data");
            int humidity = current.optInt("humidity", 0);
            double windKph = current.optDouble("wind_kph", 0);

            return placeName + ": " + tempC + " °C, " + condition
                    + ", humidity: " + humidity + "%, wind: " + windKph + " km/h";
        } catch (Exception e) {
            return "Error getting weather : " + e.getMessage();
        }
    }

    // 4. KIỂM TRA XEM TÊN THÀNH PHỐ INPUT CÓ KHỚP VỚI TÊN TRẢ VỀ TỪ API KHÔNG (HỖ TRỢ SO SÁNH MỘT PHẦN)
    private static boolean isReasonablySameCity(String input, String resolvedName) {
        String a = ValidationUtils.normalizeName(input);        // Note: input
        String b = ValidationUtils.normalizeName(resolvedName); // Note: api
        return a.equals(b) || a.contains(b) || b.contains(a);   //Note: so sánh input và api để phòng trường hợp 'ho chi minh' và 'thanh pho ho chi minh'
    }

    // 1.1. Lấy dân số của thành phố bằng cách gọi song song 2 API ,
    private static String getCityPopulation(String cityName, String countryName, double lat, double lon) {
        // Note: Gọi song song cả 2 API
        CompletableFuture<String> ninjasF = CompletableFuture.supplyAsync(
                () -> getCityPopulationFromApiNinjas(cityName, lat, lon),
                BACKGROUND_EXECUTOR
        ).exceptionally(e -> "no data");

        CompletableFuture<String> geonamesF = CompletableFuture.supplyAsync(
                () -> getCityPopulationFromGeoNames(cityName, countryName),
                BACKGROUND_EXECUTOR
        ).exceptionally(e -> "no data");
        // Note: Chờ cả 2 hoàn thành
        CompletableFuture.allOf(ninjasF, geonamesF).join();
        // Note: Ưu tiên ApiNinjas, fallback GeoNames
        String ninjasResult = ninjasF.join();
        if (!"no data".equals(ninjasResult) && !ninjasResult.startsWith("no data")) {
            return ninjasResult;
        }
        return geonamesF.join();
    }
    // 1.1.1. Gọi API Ninjas để lấy dân số thành phố dựa trên tên và tọa độ
    private static String getCityPopulationFromApiNinjas(String cityName, double targetLat, double targetLon) {
        HttpURLConnection connection = null;

        try {
            String url = API_NINJAS_CITY_API_URL
                    + "?name=" + URLEncoder.encode(cityName, StandardCharsets.UTF_8);

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("X-Api-Key", API_NINJAS_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                return "no data ( Response code: " + responseCode + ")";
            }

            String body = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONArray cities = new JSONArray(body);
            if (cities.isEmpty()) {
                return "no data";
            }

            JSONObject bestMatch = null;
            double bestDistance = Double.MAX_VALUE;

            for (int i = 0; i < cities.length(); i++) {
                JSONObject city = cities.getJSONObject(i);
                double apiLat = city.optDouble("latitude", Double.NaN);
                double apiLon = city.optDouble("longitude", Double.NaN);

                if (Double.isNaN(apiLat) || Double.isNaN(apiLon)) {
                    continue;
                }

                double distance = Math.pow(apiLat - targetLat, 2) + Math.pow(apiLon - targetLon, 2);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = city;
                }
            }

            if (bestMatch == null) {
                bestMatch = cities.getJSONObject(0);
            }

            long population = bestMatch.optLong("population", 0);
            return population > 0 ? String.valueOf(population) : "no data";
        } catch (Exception e) {
            return "no data";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    // 1.1.2. Gọi API GeoNames để lấy dân số thành phố dựa trên tên và quốc gia
    private static String getCityPopulationFromGeoNames(String cityName, String countryName) {
        try {
            String url = GEONAMES_SEARCH_API_URL
                    + "?q=" + URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                    + "&countryBias=" + URLEncoder.encode(countryName, StandardCharsets.UTF_8)
                    + "&featureClass=P"
                    + "&maxRows=10&username=" + GEONAMES_USERNAME;

            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());
            JSONArray geonames = json.optJSONArray("geonames");
            if (geonames == null || geonames.isEmpty()) {
                return "no data";
            }

            long bestPopulation = 0;
            for (int i = 0; i < geonames.length(); i++) {
                JSONObject item = geonames.getJSONObject(i);
                long population = item.optLong("population", 0);
                if (population > bestPopulation) {
                    bestPopulation = population;
                }
            }

            return bestPopulation > 0 ? String.valueOf(bestPopulation) : "no data";
        } catch (Exception e) {
            return "No data";
        }
    }

    // 1.2. Tạo JSON response cho trường hợp lỗi với status "error", type "city"
    private static String createErrorResponse(String message) {
        return new JSONObject()
                .put("status", "error")
                .put("type", "city")
                .put("message", message)
                .toString(2);
    }

    // 2.1.  Trích xuất mảng "items" từ JSON text trả về từ service khác.
    private static JSONArray extractItems(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            return json.optJSONArray("items") == null ? new JSONArray() : json.optJSONArray("items");
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
