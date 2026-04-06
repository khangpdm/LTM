package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import org.json.JSONObject;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import org.json.JSONArray;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CityService {

    public static String getCityInfo(String input) {

        // API Key cho WeatherAPI
        String key = "a7a0ef458a3642009a580805262003";

        String url = "https://api.weatherapi.com/v1/current.json?key=" + key + "&q=" + input.trim() + "&aqi=yes";
        try {
            Document doc = ApiConnector.get(url);
            String responseText = doc.text();
            JSONObject json = new JSONObject(responseText);

            // Kiểm tra nếu API trả về lỗi
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                String errorMsg = error.has("message") ? error.getString("message") : "Không tìm thấy thành phố";
                return "❌ Lỗi: " + errorMsg + "\n"
                        + "💡 Gợi ý: Kiểm tra xem tên thành phố có đúng không (ví dụ: Hanoi, Ho Chi Minh City, London)";
            }

            JSONObject location = json.getJSONObject("location");
            String name = location.getString("name");
            String region = location.has("region") ? location.getString("region") : "N/A";
            String country = location.getString("country");
            double lat = location.getDouble("lat");
            double lon = location.getDouble("lon");
            String localTime = location.getString("localtime");
            String tzId = location.has("tz_id") ? location.getString("tz_id") : "N/A";

            JSONObject current = json.getJSONObject("current");
            double tempC = current.getDouble("temp_c");
            double tempF = current.getDouble("temp_f");
            String condition = current.getJSONObject("condition").getString("text");
            // String conditionIcon = current.getJSONObject("condition").has("icon")
            // ? current.getJSONObject("condition").getString("icon")
            // : "";
            int humidity = current.getInt("humidity");
            double windKph = current.getDouble("wind_kph");
            double windMph = current.getDouble("wind_mph");
            int pressure = current.getInt("pressure_mb");
            double feelsLike = current.getDouble("feelslike_c");
            int visibility = current.getInt("vis_km");
            double uv = current.getDouble("uv");

            // Lấy tin tức
            String news = NewsService.getNewsInfo(name);
            String hotels = getTopHotels(lat, lon, name, country);

            return "===============================================\n"
                    + "📍 THÔNG TIN THÀNH PHỐ\n"
                    + "===============================================\n"
                    + "Thành phố: " + name + "\n"
                    + "Tỉnh/Vùng: " + region + "\n"
                    + "Quốc gia: " + country + "\n"
                    + "Múi giờ: " + tzId + "\n"
                    + "Tọa độ: " + lat + ", " + lon + "\n"
                    + "Thời gian địa phương: " + localTime + "\n"
                    + "\n"
                    + "🌡️ THÔNG TIN THỜI TIẾT\n"
                    + "===============================================\n"
                    + "Điều kiện: " + condition + "\n"
                    + "Nhiệt độ hiện tại: " + tempC + "°C (" + tempF + "°F)\n"
                    + "Cảm thấy như: " + feelsLike + "°C\n"
                    + "Độ ẩm: " + humidity + "%\n"
                    + "Tốc độ gió: " + windKph + " km/h (" + windMph + " mph)\n"
                    + "Áp suất: " + pressure + " mb\n"
                    + "Tầm nhìn: " + visibility + " km\n"
                    + "Chỉ số UV: " + uv + "\n"
                    + "\n"
                    + "📰 TIN TỨC LIÊN QUAN\n"
                    + "===============================================\n"
                    + news + "\n"
                    + "\n"
                    + "GỢI Ý KHÁCH SẠN NỔI BẬT\n"
                    + "===============================================\n"
                    + hotels
                    + "\n===============================================";
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage() + "\n"
                    + "💡 Gợi ý: Kiểm tra kết nối Internet hoặc thử lại sau";
        } catch (Exception e) {
            return "❌ Lỗi xử lý dữ liệu: " + e.getMessage() + "\n"
                    + "💡 Gợi ý: Kiểm tra xem tên thành phố có đúng không";
        }
    }

    private static String getTopHotels(double lat, double lon, String expectedCity, String expectedCountry) {
        String apiKey = System.getenv("RAPIDAPI_KEY");
        String apiHost = "booking-com15.p.rapidapi.com";

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("NHAP_KEY_CUA_BAN_VAO_DAY")) {
            return "[Lỗi] Bạn chưa cấu hình RapidAPI Key thành công. Vui lòng kiểm tra lại launch.json.";
        }

        try {
            // Cần truyền ngày check-in/check-out để Booking.com tính giá. Lấy ngày mai làm
            // check-in mặc định:
            String arrivalDate = java.time.LocalDate.now().plusDays(1).toString();
            String departureDate = java.time.LocalDate.now().plusDays(2).toString();

            String url = "https://" + apiHost + "/api/v1/hotels/searchHotelsByCoordinates"
                    + "?latitude=" + lat + "&longitude=" + lon
                    + "&arrival_date=" + arrivalDate + "&departure_date=" + departureDate
                    + "&adults=1&room_qty=1";

            // Gửi HTTP GET với Header theo chuẩn của Hệ thống
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .header("X-RapidAPI-Key", apiKey)
                    .header("X-RapidAPI-Host", apiHost)
                    .timeout(60000)
                    .get();

            JSONObject root = new JSONObject(doc.text());

            if (root.has("status") && !root.getBoolean("status")) {
                return "[Lỗi] API Booking.com phản hồi thất bại, vui lòng kiểm tra lại Key hoặc gói cước.";
            }

            JSONObject dataObj = root.optJSONObject("data");
            if (dataObj == null || !dataObj.has("result")) {
                return "[Thông báo] Rất tiếc, định dạng trả về bị lỗi hoặc không có dữ liệu khách sạn.";
            }

            JSONArray arr = dataObj.getJSONArray("result");

            if (arr.isEmpty()) {
                return "[Thông báo] Rất tiếc, không tìm thấy khách sạn nào từ hệ thống Booking.com cho khu vực này.";
            }

            List<JSONObject> strictMatches = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject h = arr.getJSONObject(i);

                // KIỂM TRA NGHIÊM NGẶT: Phải đúng tên thành phố
                String hotelCity = h.optString("city", "").toLowerCase().replaceAll("[\\s-]", "");
                String checkCity = expectedCity.toLowerCase().replaceAll("[\\s-]", "");

                if (!hotelCity.isEmpty() && !checkCity.isEmpty()) {
                    if (hotelCity.contains(checkCity) || checkCity.contains(hotelCity)) {
                        strictMatches.add(h);
                    }
                }
            }

            if (strictMatches.isEmpty()) {
                return "Rất tiếc, Booking.com không có phòng trực tuyến khả dụng ngay tại trung tâm \"" + expectedCity
                        + "\" vào lúc này.\n"
                        + "💡 Gợi ý: Hãy thử tìm kiếm tên Tỉnh hoặc Thành phố trung tâm lớn hơn (Ví dụ: Vung Tau thay vì Phuoc Hai).";
            }

            List<String> lines = new ArrayList<>();
            for (int i = 0; i < strictMatches.size(); i++) {
                JSONObject h = strictMatches.get(i);
                if (lines.size() >= 3)
                    break; // Chỉ hiển thị tối đa 3 khách sạn

                // Thuật toán parse do Booking-com15 trả về nhiều khối cấu trúc khác nhau
                JSONObject prop = h.optJSONObject("property");

                String name = "N/A";
                if (prop != null && prop.has("name"))
                    name = prop.getString("name");
                else if (h.has("hotel_name"))
                    name = h.getString("hotel_name");

                String address = "Gần tâm bản đồ (" + lat + ", " + lon + ")";
                if (h.has("city_in_trans"))
                    address = h.getString("city_in_trans");
                else if (h.has("distance_to_cc"))
                    address = "Cách trung tâm " + h.optString("distance_to_cc") + " km";

                // Lấy đánh giá
                String rating = "Chưa có";
                String reviews = "0";
                if (prop != null && prop.has("reviewScore")) {
                    rating = String.valueOf(prop.getDouble("reviewScore"));
                    reviews = String.valueOf(prop.getInt("reviewCount"));
                } else if (h.has("review_score")) {
                    rating = String.valueOf(h.getDouble("review_score"));
                    reviews = String.valueOf(h.optInt("review_nr", 0));
                }

                // Lấy Cấu trúc Tiền tệ nội địa (không hardcode loại tiền để tự động theo nước)
                String price = "Đang cập nhật";
                if (prop != null && prop.has("priceBreakdown") && !prop.isNull("priceBreakdown")) {
                    JSONObject gross = prop.getJSONObject("priceBreakdown").optJSONObject("grossPrice");
                    if (gross != null) {
                        price = Math.round(gross.getDouble("value")) + " " + gross.optString("currency", "USD");
                    }
                } else if (h.has("composite_price_breakdown") && !h.isNull("composite_price_breakdown")) {
                    JSONObject composite = h.getJSONObject("composite_price_breakdown");
                    if (composite.has("gross_amount_hotel_currency")
                            && !composite.isNull("gross_amount_hotel_currency")) {
                        JSONObject grossLocal = composite.getJSONObject("gross_amount_hotel_currency");
                        long pVal = Math.round(grossLocal.getDouble("value"));
                        price = String.format("%,d %s", pVal, grossLocal.optString("currency", ""));
                    } else if (composite.has("gross_amount") && !composite.isNull("gross_amount")) {
                        JSONObject gross = composite.getJSONObject("gross_amount");
                        price = Math.round(gross.getDouble("value")) + " " + gross.optString("currency", "USD");
                    }
                } else if (h.has("min_total_price")) {
                    long pVal = Math.round(h.getDouble("min_total_price"));
                    price = String.format("%,d %s", pVal, h.optString("currencycode", ""));
                }

                lines.add((i + 1) + ") " + name + "\n"
                        + "   - Địa chỉ/Vị trí: " + address + "\n"
                        + "   - Giá phòng tham khảo: " + price + " / đêm\n"
                        + "   - Điểm số: " + rating + "/10 (" + reviews + " nhận xét)");
            }

            return String.join("\n\n", lines);

        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 429) {
                return "❌ [LỖI 429 - Hết lượt API] Tài khoản RapidAPI của bạn đã vượt quá giới hạn lượt truy vấn miễn phí (hoặc bạn đang click quá nhanh). Vui lòng đợi vài phút hoặc đăng ký API Key mới để tiếp tục!";
            }
            if (e.getStatusCode() == 403 || e.getStatusCode() == 401) {
                return "[Lỗi 403/401] RapidAPI Key của bạn không hợp lệ!";
            }
            return "[Lỗi HTTP " + e.getStatusCode() + "] Không thể truy cập API Booking.com. Chi tiết: "
                    + e.getMessage();
        } catch (IOException e) {
            return "[Lỗi kết nối] Lỗi mạng khi gọi API: " + e.getMessage();
        } catch (Exception e) {
            return "[Lỗi Exception] Xử lý cục bộ thất bại: " + e.getMessage();
        }
    }

}
