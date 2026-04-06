package geoinfo.server.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HotelServiceLimit {

    private static final String API_KEY = "a968ffde8dmshc5924334016a795p178ea7jsn153760f2cd40";
    private static final String API_HOST = "booking-com15.p.rapidapi.com";
    private static final String BASE_URL = "https://booking-com15.p.rapidapi.com";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static String getHotelsByCity(String cityName) {
        try {
            JSONObject destination = searchDestination(cityName);
            if (destination == null) {
                return "Khách sạn gợi ý: không tìm thấy điểm đến phù hợp cho " + cityName;
            }

            String destId = String.valueOf(destination.opt("dest_id"));
            String searchType = destination.optString("search_type", "CITY");
            JSONObject searchResult = searchHotels(destId, searchType);
            JSONArray hotels = extractHotels(searchResult);

            if (hotels == null || hotels.isEmpty()) {
                return "Khách sạn gợi ý: không tìm thấy khách sạn cho " + cityName;
            }

            int limit = Math.min(3, hotels.length());
            List<CompletableFuture<String>> tasks = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                JSONObject hotel = hotels.getJSONObject(i);
                tasks.add(CompletableFuture.supplyAsync(() -> formatHotelItem(hotel)).exceptionally(e -> ""));
            }

            StringBuilder result = new StringBuilder("Khách sạn gợi ý cho " + cityName + ":\n");
            for (CompletableFuture<String> task : tasks) {
                String text = task.join();
                if (!text.isBlank()) {
                    result.append(text).append("\n");
                }
            }

            return result.toString();
        } catch (Exception e) {
            return "Khách sạn gợi ý: lỗi lấy dữ liệu - " + e.getMessage();
        }
    }

    private static JSONObject searchDestination(String cityName) throws Exception {
        String url = BASE_URL + "/api/v1/hotels/searchDestination?query="
                + URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8);

        JSONObject response = sendGetRequest(url);
        JSONArray data = response.optJSONArray("data");

        if (data == null || data.isEmpty()) {
            return null;
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            if ("CITY".equalsIgnoreCase(item.optString("search_type"))) {
                return item;
            }
        }

        return data.getJSONObject(0);
    }

    private static JSONObject searchHotels(String destId, String searchType) throws Exception {
        String url = BASE_URL + "/api/v1/hotels/searchHotels"
                + "?dest_id=" + URLEncoder.encode(destId, StandardCharsets.UTF_8)
                + "&search_type=" + URLEncoder.encode(searchType, StandardCharsets.UTF_8)
                + "&arrival_date=" + URLEncoder.encode(getArrivalDate(), StandardCharsets.UTF_8)
                + "&departure_date=" + URLEncoder.encode(getDepartureDate(), StandardCharsets.UTF_8)
                + "&adults=1"
                + "&children_age=0%2C17"
                + "&room_qty=1"
                + "&page_number=1"
                + "&units=metric"
                + "&temperature_unit=c"
                + "&languagecode=en-us"
                + "&currency_code=VND";

        return sendGetRequest(url);
    }

    private static JSONObject getHotelDetails(String hotelId) throws Exception {
        String url = BASE_URL + "/api/v1/hotels/getHotelDetails"
                + "?hotel_id=" + URLEncoder.encode(hotelId, StandardCharsets.UTF_8)
                + "&arrival_date=" + URLEncoder.encode(getArrivalDate(), StandardCharsets.UTF_8)
                + "&departure_date=" + URLEncoder.encode(getDepartureDate(), StandardCharsets.UTF_8)
                + "&adults=1"
                + "&children_age=0%2C17"
                + "&room_qty=1"
                + "&units=metric"
                + "&temperature_unit=c"
                + "&languagecode=en-us"
                + "&currency_code=VND";

        JSONObject response = sendGetRequest(url);
        return response.optJSONObject("data");
    }

    private static JSONArray getHotelReviews(String hotelId) throws Exception {
        String url = BASE_URL + "/api/v1/hotels/getHotelReviews"
                + "?hotel_id=" + URLEncoder.encode(hotelId, StandardCharsets.UTF_8)
                + "&languagecode=en-us"
                + "&sort_option_id=sort_recent_desc";

        JSONObject response = sendGetRequest(url);
        JSONObject data = response.optJSONObject("data");
        return data == null ? null : data.optJSONArray("result");
    }

    private static JSONObject sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", API_HOST)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return new JSONObject(response.body());
    }

    private static JSONArray extractHotels(JSONObject response) {
        JSONObject data = response.optJSONObject("data");
        if (data == null) {
            return null;
        }

        JSONArray hotels = data.optJSONArray("hotels");
        return hotels != null ? hotels : data.optJSONArray("result");
    }

    private static String formatHotelItem(JSONObject hotel) {
        JSONObject property = hotel.optJSONObject("property");
        if (property == null) {
            return "";
        }

        String hotelId = String.valueOf(property.opt("id"));
        String name = firstNonBlank(property.optString("name"), "Khách sạn không rõ tên");
        String price = extractPrice(property);
        String reviewScore = firstNonBlank(String.valueOf(property.opt("reviewScore")), "Chưa có dữ liệu");
        String reviewCount = firstNonBlank(String.valueOf(property.opt("reviewCount")), "0");

        CompletableFuture<JSONObject> detailFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return getHotelDetails(hotelId);
            } catch (Exception e) {
                System.out.println("Lỗi lấy chi tiết khách sạn " + hotelId + ": " + e.getMessage());
                return null;
            }
        });

        CompletableFuture<JSONArray> reviewFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return getHotelReviews(hotelId);
            } catch (Exception e) {
                System.out.println("Lỗi lấy đánh giá khách sạn " + hotelId + ": " + e.getMessage());
                return null;
            }
        });

        JSONObject detail = detailFuture.join();
        JSONArray reviews = reviewFuture.join();

        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(name).append("\n");
        sb.append("  Địa chỉ: ").append(buildFullAddress(detail)).append("\n");
        sb.append("  Giá phòng: ").append(price).append("\n");
        sb.append("  Đánh giá tổng: ").append(reviewScore)
                .append(" (").append(reviewCount).append(" đánh giá)\n");

        String facilities = buildFacilities(detail);
        if (!facilities.isBlank()) {
            sb.append("  Tiện ích nổi bật: ").append(facilities).append("\n");
        }

        String reviewSummary = buildReviewSummary(reviews);
        if (!reviewSummary.isBlank()) {
            sb.append(reviewSummary);
        }

        String hotelUrl = detail == null ? "" : firstNonBlank(detail.optString("url"));
        if (!hotelUrl.isBlank()) {
            sb.append("  Link: ").append(hotelUrl).append("\n");
        }

        return sb.toString();
    }

    private static String buildFullAddress(JSONObject detail) {
        if (detail == null) {
            return "Không rõ địa chỉ";
        }

        StringBuilder sb = new StringBuilder();
        appendPart(sb, detail.optString("address"));
        appendPart(sb, detail.optString("district"));
        appendPart(sb, detail.optString("city"));
        appendPart(sb, detail.optString("country_trans"));
        appendPart(sb, detail.optString("zip"));
        return sb.isEmpty() ? "Không rõ địa chỉ" : sb.toString();
    }

    private static String buildFacilities(JSONObject detail) {
        if (detail == null) {
            return "";
        }

        JSONObject facilitiesBlock = detail.optJSONObject("facilities_block");
        if (facilitiesBlock == null) {
            return "";
        }

        JSONArray facilities = facilitiesBlock.optJSONArray("facilities");
        if (facilities == null || facilities.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(4, facilities.length());

        for (int i = 0; i < limit; i++) {
            String name = facilities.getJSONObject(i).optString("name").trim();
            if (name.isBlank()) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(name);
        }

        return sb.toString();
    }

    private static String buildReviewSummary(JSONArray reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(2, reviews.length());

        for (int i = 0; i < limit; i++) {
            JSONObject review = reviews.getJSONObject(i);
            JSONObject author = review.optJSONObject("author");

            String authorName = author == null ? "Ẩn danh" : firstNonBlank(author.optString("name"), "Ẩn danh");
            String title = firstNonBlank(review.optString("title"));
            String pros = firstNonBlank(review.optString("pros"));
            String cons = firstNonBlank(review.optString("cons"));

            sb.append("  Nhận xét ").append(i + 1).append(": ").append(authorName);
            if (!title.isBlank()) {
                sb.append(" - ").append(title);
            }
            sb.append("\n");

            if (!pros.isBlank()) {
                sb.append("    Điểm tốt: ").append(pros).append("\n");
            }

            if (!cons.isBlank()) {
                sb.append("    Điểm chưa tốt: ").append(cons).append("\n");
            }
        }

        return sb.toString();
    }

    private static String extractPrice(JSONObject property) {
        JSONObject priceBreakdown = property.optJSONObject("priceBreakdown");
        if (priceBreakdown == null) {
            return "Chưa có dữ liệu";
        }

        JSONObject grossPrice = priceBreakdown.optJSONObject("grossPrice");
        if (grossPrice != null) {
            double value = grossPrice.optDouble("value", -1);
            String currency = firstNonBlank(grossPrice.optString("currency"), "VND");
            if (value >= 0) {
                return formatMoney(value) + " " + currency;
            }
        }

        JSONObject strikePrice = priceBreakdown.optJSONObject("strikethroughPrice");
        if (strikePrice != null) {
            double value = strikePrice.optDouble("value", -1);
            String currency = firstNonBlank(strikePrice.optString("currency"), "VND");
            if (value >= 0) {
                return formatMoney(value) + " " + currency;
            }
        }

        return "Chưa có dữ liệu";
    }

    private static String getArrivalDate() {
        return LocalDate.now().plusDays(7).toString();
    }

    private static String getDepartureDate() {
        return LocalDate.now().plusDays(9).toString();
    }

    private static String formatMoney(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        return decimalFormat.format(Math.round(value)).replace(',', '.');
    }

    private static void appendPart(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(value.trim());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return "";
    }

    public static void main(String[] args) {
        System.out.println(getHotelsByCity("Đà Nẵng"));
    }
}
