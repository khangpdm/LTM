package geoinfo.service;

import geoinfo.utils.ApiConnector;
import geoinfo.service.NewsService;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import java.io.IOException;

public class CityService {

    public static String getCityInfo(String input) {

        //Co the thay doi
        String key = "a7a0ef458a3642009a580805262003";

        String url = "https://api.weatherapi.com/v1/current.json?key=" + key + "&q=" + input.trim() + "&aqi=no" ;
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

            String news = NewsService.getNewsInfo(name);

            return  "===============================================\n"
                    + "Thành phố: " + name + "\n"
                    + "Vùng: " + region + "\n"
                    + "Quốc gia: " + country + "\n"
                    + "Tọa độ: " + lat + ", " + lon + "\n"
                    + "Thời gian địa phương: " + localTime + "\n"
                    + "Nhiệt độ hiện tại: " + tempC + "°C\n"
                    + "Thời tiết: " + condition + "\n"
                    + "Độ ẩm: " + humidity + "%\n"
                    + "Gió: " + windKph + " km/h\n"
                    + "===============================================\n"
                    + "TIN TỨC" + "\n"
                    + news;
        } catch (IOException e){
            return "Lỗi khi lấy dữ liệu thành phố: " + e.getMessage();
        }
    }
}
