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
        JSONObject response = new JSONObject()
                .put("status", "success")
                .put("type", "hotels")
                .put("city", cityName == null ? "" : cityName.trim())
                .put("items", new JSONArray());

        if (cityName == null || cityName.isBlank()) {
            return response.put("status", "error")
                    .put("message", "City name is required.")
                    .toString(2);
        }

        try {
            JSONObject destination = searchDestination(cityName);
            if (destination == null) {
                return response.put("message", "No matching destination found.").toString(2);
            }

            String destId = String.valueOf(destination.opt("dest_id"));
            String searchType = destination.optString("search_type", "CITY");
            JSONObject searchResult = searchHotels(destId, searchType);
            JSONArray hotels = extractHotels(searchResult);

            if (hotels == null || hotels.isEmpty()) {
                return response.put("message", "No hotels found.").toString(2);
            }

            int limit = Math.min(3, hotels.length());
            List<CompletableFuture<JSONObject>> tasks = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                JSONObject hotel = hotels.getJSONObject(i);
                tasks.add(CompletableFuture.supplyAsync(() -> formatHotelItem(hotel)).exceptionally(e -> null));
            }

            JSONArray items = new JSONArray();
            for (CompletableFuture<JSONObject> task : tasks) {
                JSONObject item = task.join();
                if (item != null) {
                    items.put(item);
                }
            }

            response.put("items", items);
            if (items.isEmpty()) {
                response.put("message", "No valid hotel data found.");
            }

            return response.toString(2);
        } catch (Exception e) {
            return response.put("status", "error")
                    .put("message", "Failed to fetch hotels: " + e.getMessage())
                    .toString(2);
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
        JSONArray reviews = data == null ? null : data.optJSONArray("result");
        return reviews == null ? new JSONArray() : reviews;
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

    private static JSONObject formatHotelItem(JSONObject hotel) {
        JSONObject property = hotel.optJSONObject("property");
        if (property == null) {
            return null;
        }

        String hotelId = String.valueOf(property.opt("id"));
        String name = firstNonBlank(property.optString("name"), "Unknown");
        String price = extractPrice(property);
        String reviewScore = firstNonBlank(String.valueOf(property.opt("reviewScore")), "Unknown");
        String reviewCount = firstNonBlank(String.valueOf(property.opt("reviewCount")), "0");

        CompletableFuture<JSONObject> detailFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return getHotelDetails(hotelId);
            } catch (Exception e) {
                return null;
            }
        });

        CompletableFuture<JSONArray> reviewFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return getHotelReviews(hotelId);
            } catch (Exception e) {
                return new JSONArray();
            }
        });

        JSONObject detail = detailFuture.join();
        JSONArray reviews = reviewFuture.join();

        return new JSONObject()
                .put("name", name)
                .put("address", buildFullAddress(detail))
                .put("price", price)
                .put("reviewScore", reviewScore)
                .put("reviewCount", reviewCount)
                .put("facilities", buildFacilities(detail))
                .put("url", detail == null ? "" : firstNonBlank(detail.optString("url")))
                .put("reviews", buildReviewItems(reviews));
    }

    private static String buildFullAddress(JSONObject detail) {
        if (detail == null) {
            return "Unknown";
        }

        StringBuilder sb = new StringBuilder();
        appendPart(sb, detail.optString("address"));
        appendPart(sb, detail.optString("district"));
        appendPart(sb, detail.optString("city"));
        appendPart(sb, detail.optString("country_trans"));
        appendPart(sb, detail.optString("zip"));
        return sb.isEmpty() ? "Unknown" : sb.toString();
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

    private static JSONArray buildReviewItems(JSONArray reviews) {
        JSONArray items = new JSONArray();
        if (reviews == null || reviews.isEmpty()) {
            return items;
        }

        int limit = Math.min(2, reviews.length());
        for (int i = 0; i < limit; i++) {
            JSONObject review = reviews.getJSONObject(i);
            JSONObject author = review.optJSONObject("author");

            String authorName = author == null ? "Anonymous" : firstNonBlank(author.optString("name"), "Anonymous");
            String title = firstNonBlank(review.optString("title"));
            String pros = firstNonBlank(review.optString("pros"));
            String cons = firstNonBlank(review.optString("cons"));

            items.put(new JSONObject()
                    .put("author", authorName)
                    .put("title", title)
                    .put("pros", pros)
                    .put("cons", cons));
        }

        return items;
    }

    private static String extractPrice(JSONObject property) {
        JSONObject priceBreakdown = property.optJSONObject("priceBreakdown");
        if (priceBreakdown == null) {
            return "Unknown";
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

        return "Unknown";
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
        System.out.println(getHotelsByCity("Da Nang"));
    }
}
