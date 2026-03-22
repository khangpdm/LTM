package geoinfo.service;

import geoinfo.utils.ApiConnector;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.nodes.Document;

public class CountryService {

    public static String getCountryInfo(String input) {
        String url = "https://restcountries.com/v3.1/translation/" + input.trim();
        try {
            // Gọi API qua ApiConnector
            Document doc = ApiConnector.get(url);

            // Parse JSON từ Document
            JSONArray arr = new JSONArray(doc.text());
            JSONObject json = arr.getJSONObject(0);

            // Lấy tọa độ
            JSONArray latlngArr = json.getJSONArray("latlng");
            String latlng = latlngArr.toString();

            // Lấy dân số
            String population = String.valueOf(json.getLong("population"));

            // Lấy tiền tệ
            StringBuilder currency = new StringBuilder();
            JSONObject currencies = json.getJSONObject("currencies");
            for (String code : currencies.keySet()) {
                JSONObject currencytemp = currencies.getJSONObject(code);
                String name = currencytemp.getString("name");
                String symbol = currencytemp.getString("symbol");
                currency.append(name).append(" - ").append(symbol).append(", ");
            }

            // Lấy ngôn ngữ
            StringBuilder language = new StringBuilder();
            JSONObject languages = json.getJSONObject("languages");
            for (String code : languages.keySet()) {
                String languagetemp = languages.getString(code);
                language.append(languagetemp).append(", ");
            }

            // Lấy quốc kỳ
            String flag = json.getString("flag");

            // Lấy biên giới (có thể không có)
            String borders = json.has("borders")
                    ? json.getJSONArray("borders").toString()
                    : "Không có";

            // Ghép kết quả trả về
            return "Tọa độ: " + latlng + "\n"
                    + "Dân số: " + population + "\n"
                    + "Đơn vị tiền tệ: " + currency + "\n"
                    + "Ngôn ngữ: " + language + "\n"
                    + "Quốc kỳ: " + flag + "\n"
                    + "Quốc gia láng giềng: " + borders;

        } catch (Exception e) {
            return "Lỗi khi lấy dữ liệu quốc gia: " + e.getMessage();
        }
    }
}
