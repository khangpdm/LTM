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
import java.text.Normalizer;

public class CityService {
    private static final String WEATHER_API_KEY = "a7a0ef458a3642009a580805262003";
    private static final String API_NINJAS_KEY = "DgGav2PqKdwGSy3B82AaE4cdRFtnE7qNFcYF4Yj1";
    private static final String GEONAMES_USERNAME = "tinmi2005";

    public static String getCityInfo(String input) {
        try {
            return getCityDetails(input).summary();
        } catch (Exception e) {
            return "Loi khi lay du lieu thanh pho: " + e.getMessage();
        }
    }

    public static String getCityNews(String input) {
        try {
            CityDetails details = getCityDetails(input);
            return "===============================================\n"
                    + "TIN TUC: " + details.name() + "\n"
                    + details.news();
        } catch (Exception e) {
            return "Loi khi lay tin tuc thanh pho: " + e.getMessage();
        }
    }

    public static CityDetails getCityDetails(String input) throws Exception {
        String validatedInput = validateInput(input);
        String url = "https://api.weatherapi.com/v1/current.json?key="
                + WEATHER_API_KEY
                + "&q="
                + URLEncoder.encode(validatedInput.trim(), StandardCharsets.UTF_8)
                + "&aqi=no";

        Document doc = ApiConnector.get(url);
        JSONObject json = new JSONObject(doc.text());

        JSONObject location = json.optJSONObject("location");
        if (location == null) {
            throw new IllegalArgumentException("vi tri khong hop le.");
        }

        String name = location.optString("name");
        if (!isReasonablySameCity(validatedInput, name)) {
            throw new IllegalArgumentException("khong tim thay thanh pho phu hop.");
        }

        String region = location.optString("region");
        String country = location.optString("country");
        double lat = location.optDouble("lat");
        double lon = location.optDouble("lon");
        String localTime = location.optString("localtime");

        JSONObject current = json.optJSONObject("current");
        double tempC = current == null ? 0 : current.optDouble("temp_c");
        JSONObject conditionObj = current == null ? null : current.optJSONObject("condition");
        String condition = conditionObj == null ? "Khong co" : conditionObj.optString("text", "Khong co");
        int humidity = current == null ? 0 : current.optInt("humidity");
        double windKph = current == null ? 0 : current.optDouble("wind_kph");

        String population = getCityPopulation(name, country, lat, lon);
        String hotels = HotelService.getHotelsByCity(name);
        String news = NewsService.getNewsInfo(name);

        String summary = "===============================================\n"
                + "THANH PHO: " + name + "\n"
                + "Vung: " + region + "\n"
                + "Quoc gia: " + country + "\n"
                + "Dan so: " + population + "\n"
                + "Toa do: " + lat + ", " + lon + "\n"
                + "Thoi gian dia phuong: " + localTime + "\n"
                + "Nhiet do hien tai: " + tempC + " do C\n"
                + "Thoi tiet: " + condition + "\n"
                + "Do am: " + humidity + "%\n"
                + "Gio: " + windKph + " km/h\n"
                + "===============================================\n"
                + hotels;

        return new CityDetails(name, summary, news);
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

            return placeName + ": " + tempC + " do C, " + condition
                    + ", do am " + humidity + "%, gio " + windKph + " km/h";
        } catch (Exception e) {
            return "Chua lay duoc du lieu thoi tiet: " + e.getMessage();
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
            throw new IllegalArgumentException("ten thanh pho qua ngan.");
        }
        if (input.length() > 100) {
            throw new IllegalArgumentException("ten thanh pho qua dai.");
        }
        if (!ValidationUtils.isValidLocationName(input)) {
            throw new IllegalArgumentException("ten thanh pho chua ky tu khong hop le.");
        }

        return input;
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

    public record CityDetails(String name, String summary, String news) {
    }
}
