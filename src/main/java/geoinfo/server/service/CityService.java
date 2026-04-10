package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
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
            return "Lỗi khi lấy dữ liệu thành phố: dữ liệu đầu vào rỗng.";
        }

        input = input.replaceAll("\\s+", " ").trim();

        if (input.isEmpty()) {
            return "Lỗi khi lấy dữ liệu thành phố: dữ liệu đầu vào rỗng.";
        }

        if (input.length() < 2) {
            return "Lỗi khi lấy dữ liệu thành phố: tên thành phố quá ngắn.";
        }

        if (input.length() > 100) {
            return "Lỗi khi lấy dữ liệu thành phố: tên thành phố quá dài.";
        }

        if (!input.matches("[\\p{L}\\p{M}0-9 .,'()\\-]+")) {
            return "Lỗi khi lấy dữ liệu thành phố: tên thành phố chứa ký tự không hợp lệ.";
        }

        String url = "https://api.weatherapi.com/v1/current.json?key="
                + WEATHER_API_KEY
                + "&q="
                + URLEncoder.encode(input.trim(), StandardCharsets.UTF_8)
                + "&aqi=no";
        try {
            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());

            JSONObject location = json.getJSONObject("location");
            String name = location.getString("name");
            if (!isReasonablySameCity(input, name)) {
                return "Lỗi khi lấy dữ liệu thành phố: không tìm thấy thành phố phù hợp.";
            }
            String region = location.getString("region");
            String country = location.getString("country");
            double lat = location.getDouble("lat");
            double lon = location.getDouble("lon");
            String localTime = location.getString("localtime");

            JSONObject current = json.getJSONObject("current");
            double tempC = current.getDouble("temp_c");
            String condition = current.getJSONObject("condition").getString("text");
            int humidity = current.getInt("humidity");
            double windKph = current.getDouble("wind_kph");

            String population = getCityPopulation(name, country, lat, lon);
            String news = NewsService.getNewsInfo(name);
            String hotels = HotelService.getHotelsByCity(name);

            return "===============================================\n"
                    + "THÀNH PHỐ: " + name + "\n"
                    + "Vùng: " + region + "\n"
                    + "Quoc gia: " + country + "\n"
                    + "Dân số: " + population + "\n"
                    + "Tọa độ: " + lat + ", " + lon + "\n"
                    + "Thời gian địa phương: " + localTime + "\n"
                    + "Nhiệt độ hiện tại: " + tempC + " °C\n"
                    + "Thời tiết: " + condition + "\n"
                    + "Độ ẩm: " + humidity + "%\n"
                    + "Gió: " + windKph + " km/h\n"
                    + "===============================================\n"
                    + "TIN TỨC\n"
                    + news + "\n"
                    + "===============================================\n"
                    + hotels;

        } catch (IOException e) {
            return "Lỗi khi lấy dữ liệu thành phố: " + e.getMessage();
        }
    }

    public static String getWeatherSummary(String query) {
        if (query == null) {
            return "Chưa có dữ liệu thời tiết.";
        }

        query = query.replaceAll("\\s+", " ").trim();

        if (query.isEmpty()) {
            return "Chưa có dữ liệu thời tiết.";
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
                    ? "Chưa có dữ liệu"
                    : current.getJSONObject("condition").optString("text", "Chưa có dữ liệu");
            int humidity = current.optInt("humidity", 0);
            double windKph = current.optDouble("wind_kph", 0);

            return placeName + ": " + tempC + " °C, " + condition
                    + ", độ ẩm " + humidity + "%, gió " + windKph + " km/h";
        } catch (Exception e) {
            return "Chưa lấy được dữ liệu thời tiết: " + e.getMessage();
        }
    }

    private static String getCityPopulation(String cityName, String countryName, double lat, double lon) {
        String population = getCityPopulationFromApiNinjas(cityName, lat, lon);
        if (!"Chưa có dữ liệu".equals(population)) {
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

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Chưa có dữ liệu";
            }

            String body = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONArray cities = new JSONArray(body);
            if (cities.isEmpty()) {
                return "Chưa có dữ liệu";
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
            return population > 0 ? String.valueOf(population) : "Chưa có dữ liệu";
        } catch (Exception e) {
            return "Chưa có dữ liệu";
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
                return "Chưa có dữ liệu";
            }

            long bestPopulation = 0;
            for (int i = 0; i < geonames.length(); i++) {
                JSONObject item = geonames.getJSONObject(i);
                long population = item.optLong("population", 0);
                if (population > bestPopulation) {
                    bestPopulation = population;
                }
            }

            return bestPopulation > 0 ? String.valueOf(bestPopulation) : "Chưa có dữ liệu";
        } catch (Exception e) {
            return "Chưa có dữ liệu";
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
}
