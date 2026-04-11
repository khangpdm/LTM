package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public class CityService {
    private static final String WEATHER_API_KEY = "a7a0ef458a3642009a580805262003";
    private static final String API_NINJAS_KEY = "DgGav2PqKdwGSy3B82AaE4cdRFtnE7qNFcYF4Yj1";
    private static final String GEONAMES_USERNAME = "tinmi2005";

    public static String getCityInfo(String input) {
        if (input == null) {
            return createErrorResponse("Loi khi lay du lieu thanh pho: du lieu dau vao rong.");
        }

        input = input.replaceAll("\\s+", " ").trim();

        if (input.isEmpty()) {
            return createErrorResponse("Loi khi lay du lieu thanh pho: du lieu dau vao rong.");
        }

        if (input.length() < 2) {
            return createErrorResponse("Loi khi lay du lieu thanh pho: ten thanh pho qua ngan.");
        }

        if (input.length() > 100) {
            return createErrorResponse("Loi khi lay du lieu thanh pho: ten thanh pho qua dai.");
        }

        if (!ValidationUtils.isValidLocationName(input)) {
            return createErrorResponse("Loi khi lay du lieu thanh pho: ten thanh pho chua ky tu khong hop le.");
        }

        String url = "https://api.weatherapi.com/v1/current.json?key="
                + WEATHER_API_KEY
                + "&q="
                + URLEncoder.encode(input.trim(), StandardCharsets.UTF_8)
                + "&aqi=no";

        try {
            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());

            JSONObject location = json.optJSONObject("location");
            if (location == null) {
                return createErrorResponse("Loi khi lay du lieu thanh pho: vi tri khong hop le.");
            }

            String name = location.optString("name");
            if (!isReasonablySameCity(input, name)) {
                return createErrorResponse("Loi khi lay du lieu thanh pho: khong tim thay thanh pho phu hop.");
            }

            String region = location.optString("region");
            String country = location.optString("country");
            double latitude = location.optDouble("lat");
            double longitude = location.optDouble("lon");
            String localTime = location.optString("localtime");

            JSONObject current = json.optJSONObject("current");
            if (current == null) {
                return createErrorResponse("Loi khi lay du lieu thanh pho: thieu du lieu thoi tiet.");
            }

            double temperatureCelsius = current.optDouble("temp_c");
            JSONObject conditionJson = current.optJSONObject("condition");
            String weatherCondition = conditionJson == null ? "" : conditionJson.optString("text");
            int humidity = current.optInt("humidity");
            double windKph = current.optDouble("wind_kph");

            String population = getCityPopulation(name, country, latitude, longitude);
            String newsJson = NewsService.getNewsInfo(name);
            String hotelsJson = HotelService.getHotelsByCity(name);

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "city");
            response.put("name", name);
            response.put("region", region);
            response.put("country", country);
            response.put("population", population);
            response.put("latitude", latitude);
            response.put("longitude", longitude);
            response.put("localTime", localTime);
            response.put("temperatureCelsius", temperatureCelsius);
            response.put("weatherCondition", weatherCondition);
            response.put("humidity", humidity);
            response.put("windKph", windKph);
            response.put("news", extractItems(newsJson));
            response.put("hotels", extractItems(hotelsJson));
            return response.toString(2);
        } catch (IOException e) {
            return createErrorResponse("Loi khi lay du lieu thanh pho: " + e.getMessage());
        }
    }

    public static String getWeatherSummary(String query) {
        if (query == null) {
            return "Chua co du lieu thoi tiet.";
        }

        query = query.replaceAll("\\s+", " ").trim();

        if (query.isEmpty()) {
            return "Chua co du lieu thoi tiet.";
        }

        String url = "https://api.weatherapi.com/v1/current.json?key="
                + WEATHER_API_KEY
                + "&q="
                + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                + "&aqi=no";
        try {
            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());

            JSONObject location = json.getJSONObject("location");
            JSONObject current = json.getJSONObject("current");

            String placeName = location.optString("name", query);
            double tempC = current.optDouble("temp_c", 0);
            String condition = current.optJSONObject("condition") == null
                    ? "Chua co du lieu"
                    : current.getJSONObject("condition").optString("text", "Chua co du lieu");
            int humidity = current.optInt("humidity", 0);
            double windKph = current.optDouble("wind_kph", 0);

            return placeName + ": " + tempC + " C, " + condition
                    + ", do am " + humidity + "%, gio " + windKph + " km/h";
        } catch (Exception e) {
            return "Chua lay duoc du lieu thoi tiet: " + e.getMessage();
        }
    }

    private static String getCityPopulation(String cityName, String countryName, double lat, double lon) {
        String population = getCityPopulationFromApiNinjas(cityName, lat, lon);
        if (!"Chua co du lieu".equals(population)) {
            return population;
        }

        return getCityPopulationFromGeoNames(cityName, countryName);
    }

    private static String getCityPopulationFromApiNinjas(String cityName, double targetLat, double targetLon) {
        HttpURLConnection connection = null;

        try {
            String url = "https://api.api-ninjas.com/v1/city?name="
                    + URLEncoder.encode(cityName, StandardCharsets.UTF_8);

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("X-Api-Key", API_NINJAS_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                return "Chua co du lieu (ma: " + responseCode + ")";
            }

            String body = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONArray cities = new JSONArray(body);
            if (cities.isEmpty()) {
                return "Chua co du lieu";
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
            return population > 0 ? String.valueOf(population) : "Chua co du lieu";
        } catch (Exception e) {
            return "Chua co du lieu";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String getCityPopulationFromGeoNames(String cityName, String countryName) {
        try {
            String url = "http://api.geonames.org/searchJSON?q="
                    + URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                    + "&countryBias="
                    + URLEncoder.encode(countryName, StandardCharsets.UTF_8)
                    + "&featureClass=P"
                    + "&maxRows=10&username="
                    + GEONAMES_USERNAME;

            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());
            JSONArray geonames = json.optJSONArray("geonames");
            if (geonames == null || geonames.isEmpty()) {
                return "Chua co du lieu";
            }

            long bestPopulation = 0;
            for (int i = 0; i < geonames.length(); i++) {
                JSONObject item = geonames.getJSONObject(i);
                long population = item.optLong("population", 0);
                if (population > bestPopulation) {
                    bestPopulation = population;
                }
            }

            return bestPopulation > 0 ? String.valueOf(bestPopulation) : "Chua co du lieu";
        } catch (Exception e) {
            return "Chua co du lieu";
        }
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');

        return normalized.toLowerCase()
                .replaceAll("[^\\p{L}0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isReasonablySameCity(String input, String resolvedName) {
        String a = normalizeName(input);
        String b = normalizeName(resolvedName);

        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    private static String createErrorResponse(String message) {
        return new JSONObject()
                .put("status", "error")
                .put("type", "city")
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