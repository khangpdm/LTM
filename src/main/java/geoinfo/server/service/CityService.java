package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CityService {
    private static final String WEATHER_API_KEY = "a7a0ef458a3642009a580805262003";
    private static final String GEONAMES_USERNAME = "tinmi2005";

    public static String getCityInfo(String input) {
        if (input == null || input.isBlank()) {
            return "Lỗi khi lấy dữ liệu thành phố: dữ liệu đầu vào rỗng.";
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

            String population = getCityPopulation(name);
            String news = NewsService.getNewsInfo(name);
            String hotels = HotelService.getHotelsByCity(name);

            return "===============================================\n"
                    + "THÀNH PHỐ: " + name + "\n"
                    + "Vùng: " + region + "\n"
                    + "Quốc gia: " + country + "\n"
                    + "Dân số: " + population + "\n"
                    + "Tọa độ: " + lat + ", " + lon + "\n"
                    + "Thời gian địa phương: " + localTime + "\n"
                    + "Nhiệt độ hiện tại: " + tempC + "°C\n"
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
        if (query == null || query.isBlank()) {
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

            return placeName + ": " + tempC + "°C, " + condition
                    + ", độ ẩm " + humidity + "%, gió " + windKph + " km/h";
        } catch (Exception e) {
            return "Chưa lấy được dữ liệu thời tiết: " + e.getMessage();
        }
    }

    private static String getCityPopulation(String cityName) {
        try {
            String url = "http://api.geonames.org/searchJSON?q="
                    + URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                    + "&maxRows=1&username="
                    + GEONAMES_USERNAME;

            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());
            JSONArray geonames = json.optJSONArray("geonames");
            if (geonames == null || geonames.isEmpty()) {
                return "Chưa có dữ liệu";
            }

            long population = geonames.getJSONObject(0).optLong("population", 0);
            return population > 0 ? String.valueOf(population) : "Chưa có dữ liệu";
        } catch (Exception e) {
            return "Chưa có dữ liệu";
        }
    }
}
