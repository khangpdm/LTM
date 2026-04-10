package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.Scanner;

public class HotelService {

    private static final String API_KEY = "a7c2d91ec1msh146f0a8ec35cc8dp11944bjsn982b0aad2229";
    private static final String API_HOST = "booking-com.p.rapidapi.com";
    private static final String BASE_URL = "https://booking-com.p.rapidapi.com/v1/hotels";
    private static final Map<String, String> API_HEADERS = Map.of(
            "x-rapidapi-key", API_KEY,
            "x-rapidapi-host", API_HOST,
            "Accept", "application/json"
    );

    private static final int MAX_HOTELS = 3;
    private static final int MAX_REVIEWS = 3;
    private static final int MAX_PROS_LENGTH = 220;
    private static final int MAX_CONS_LENGTH = 180;

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
                return response.put("message", "No matching destination found.")
                        .toString(2);
            }

            String destId = firstNonBlank(
                    destination.opt("dest_id") == null ? "" : String.valueOf(destination.opt("dest_id")),
                    destination.optString("destId")
            );
            String destType = normalizeDestType(
                    firstNonBlank(destination.optString("dest_type"), destination.optString("destType"), "city")
            );

            if (destId.isBlank()) {
                return response.put("status", "error")
                        .put("message", "Destination id is missing.")
                        .toString(2);
            }

            JSONObject searchResult = searchHotels(destId, destType);
            JSONArray hotels = searchResult.optJSONArray("result");
            if (hotels == null || hotels.isEmpty()) {
                return response.put("message", "No hotels found.")
                        .toString(2);
            }

            JSONArray hotelItems = new JSONArray();
            for (int i = 0; i < hotels.length() && hotelItems.length() < MAX_HOTELS; i++) {
                JSONObject hotel = hotels.optJSONObject(i);
                if (hotel == null) {
                    continue;
                }

                JSONObject formattedHotel = buildHotelItem(hotel);
                if (formattedHotel == null) {
                    continue;
                }
                hotelItems.put(formattedHotel);
            }

            response.put("items", hotelItems);
            if (hotelItems.isEmpty()) {
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
        String url = BASE_URL + "/locations"
                + "?name=" + URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8)
                + "&locale=en-gb";

        JSONArray data = sendGetRequestArray(url);
        if (data == null || data.isEmpty()) {
            return null;
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String destType = normalizeDestType(
                    firstNonBlank(item.optString("dest_type"), item.optString("destType"))
            );
            if ("city".equals(destType)) {
                return item;
            }
        }

        return data.optJSONObject(0);
    }

    private static JSONObject searchHotels(String destId, String destType) throws Exception {
        String url = BASE_URL + "/search"
                + "?dest_id=" + URLEncoder.encode(destId, StandardCharsets.UTF_8)
                + "&dest_type=" + URLEncoder.encode(destType, StandardCharsets.UTF_8)
                + "&checkin_date=" + URLEncoder.encode(getArrivalDate(), StandardCharsets.UTF_8)
                + "&checkout_date=" + URLEncoder.encode(getDepartureDate(), StandardCharsets.UTF_8)
                + "&adults_number=1"
                + "&room_number=1"
                + "&order_by=popularity"
                + "&filter_by_currency=VND"
                + "&locale=en-gb"
                + "&units=metric"
                + "&page_number=0"
                + "&include_adjacency=true";

        return sendGetRequestObject(url);
    }

    private static JSONObject sendGetRequestObject(String url) throws Exception {
        String body = ApiConnector.getJsonText(url, API_HEADERS);
        return new JSONObject(body);
    }

    private static JSONArray sendGetRequestArray(String url) throws Exception {
        String body = ApiConnector.getJsonText(url, API_HEADERS);
        return new JSONArray(body);
    }

    private static JSONObject buildHotelItem(JSONObject hotel) {
        String name = firstNonBlank(
                hotel.optString("hotel_name"),
                hotel.optString("hotel_name_trans"),
                hotel.optString("name")
        );
        if (name.isBlank()) {
            return null;
        }

        String address = buildAddress(hotel);
        String price = extractPrice(hotel);
        String reviewScore = extractReviewScore(hotel);
        String reviewCount = extractReviewCount(hotel);
        String hotelClass = extractHotelClass(hotel);
        String hotelUrl = normalizeUrl(firstNonBlank(hotel.optString("url"), hotel.optString("hotel_url")));
        String hotelId = hotel.opt("hotel_id") == null ? "" : String.valueOf(hotel.opt("hotel_id"));

        return new JSONObject()
                .put("name", name)
                .put("address", address)
                .put("price", price)
                .put("reviewScore", reviewScore)
                .put("reviewCount", reviewCount)
                .put("hotelClass", hotelClass)
                .put("url", hotelUrl)
                .put("reviews", hotelId.isBlank() ? new JSONArray() : getHotelReviews(hotelId));
    }

    private static String buildAddress(JSONObject hotel) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, hotel.optString("address"));
        appendPart(sb, hotel.optString("district"));
        appendPart(sb, hotel.optString("city"));
        appendPart(sb, hotel.optString("country_trans"));
        appendPart(sb, hotel.optString("zip"));
        return sb.isEmpty() ? "Unknown" : sb.toString();
    }

    private static String extractPrice(JSONObject hotel) {
        double minPrice = hotel.optDouble("min_total_price", -1);
        if (minPrice >= 0) {
            return formatMoney(minPrice) + " " + firstNonBlank(hotel.optString("currencycode"), "VND");
        }

        JSONObject compositePrice = hotel.optJSONObject("composite_price_breakdown");
        if (compositePrice != null) {
            JSONObject grossAmount = compositePrice.optJSONObject("gross_amount");
            if (grossAmount != null) {
                double value = grossAmount.optDouble("value", -1);
                String currency = firstNonBlank(grossAmount.optString("currency"), "VND");
                if (value >= 0) {
                    return formatMoney(value) + " " + currency;
                }
            }
        }

        JSONObject priceBreakdown = hotel.optJSONObject("price_breakdown");
        if (priceBreakdown != null) {
            JSONObject grossPrice = priceBreakdown.optJSONObject("gross_price");
            if (grossPrice != null) {
                double value = grossPrice.optDouble("value", -1);
                String currency = firstNonBlank(grossPrice.optString("currency"), "VND");
                if (value >= 0) {
                    return formatMoney(value) + " " + currency;
                }
            }
        }

        return "Unknown";
    }

    private static String extractReviewScore(JSONObject hotel) {
        double score = hotel.optDouble("review_score", -1);
        if (score >= 0) {
            return String.valueOf(score);
        }

        String scoreText = firstNonBlank(hotel.optString("review_score_word"), hotel.optString("reviewScore"));
        return scoreText.isBlank() ? "Unknown" : scoreText;
    }

    private static String extractReviewCount(JSONObject hotel) {
        Object reviewNr = hotel.opt("review_nr");
        if (reviewNr != null && !"null".equalsIgnoreCase(String.valueOf(reviewNr))) {
            return String.valueOf(reviewNr);
        }

        Object reviewCount = hotel.opt("review_count");
        if (reviewCount != null && !"null".equalsIgnoreCase(String.valueOf(reviewCount))) {
            return String.valueOf(reviewCount);
        }

        return "";
    }

    private static String extractHotelClass(JSONObject hotel) {
        Object star = hotel.opt("class");
        if (star != null && !"null".equalsIgnoreCase(String.valueOf(star))) {
            return String.valueOf(star);
        }

        return firstNonBlank(hotel.optString("class_is_estimated"));
    }

    private static String normalizeDestType(String value) {
        if (value == null || value.isBlank()) {
            return "city";
        }
        return value.trim().toLowerCase();
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/")) {
            return "https://www.booking.com" + url;
        }
        return "https://www.booking.com/" + url;
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

    private static JSONArray getHotelReviews(String hotelId) {
        JSONArray reviewItems = new JSONArray();

        try {
            String url = BASE_URL + "/reviews"
                    + "?hotel_id=" + URLEncoder.encode(hotelId, StandardCharsets.UTF_8)
                    + "&locale=en-gb"
                    + "&page_number=0"
                    + "&sort_type=SORT_MOST_RELEVANT";

            JSONObject response = sendGetRequestObject(url);
            JSONArray reviews = extractReviews(response);
            if (reviews == null || reviews.isEmpty()) {
                return reviewItems;
            }

            int count = Math.min(MAX_REVIEWS, reviews.length());
            for (int i = 0; i < count; i++) {
                JSONObject review = reviews.optJSONObject(i);
                if (review == null) {
                    continue;
                }

                String author = extractAuthor(review);
                String title = review.optString("title", "");
                String pros = shorten(review.optString("pros", ""), MAX_PROS_LENGTH);
                String cons = shorten(review.optString("cons", ""), MAX_CONS_LENGTH);

                if (author.isBlank() && title.isBlank() && pros.isBlank() && cons.isBlank()) {
                    continue;
                }

                reviewItems.put(new JSONObject()
                        .put("author", author)
                        .put("title", title)
                        .put("pros", pros)
                        .put("cons", cons));
            }
        } catch (Exception ignored) {
            return reviewItems;
        }

        return reviewItems;
    }

    private static JSONArray extractReviews(JSONObject response) {
        JSONArray reviews = response.optJSONArray("result");
        if (reviews != null) {
            return reviews;
        }

        reviews = response.optJSONArray("reviews");
        if (reviews != null) {
            return reviews;
        }

        JSONObject data = response.optJSONObject("data");
        if (data != null) {
            reviews = data.optJSONArray("result");
            if (reviews != null) {
                return reviews;
            }

            reviews = data.optJSONArray("reviews");
            if (reviews != null) {
                return reviews;
            }
        }

        return null;
    }

    private static String extractAuthor(JSONObject review) {
        Object author = review.opt("author");
        if (author instanceof JSONObject authorObject) {
            return firstNonBlank(
                    authorObject.optString("name"),
                    authorObject.optString("display_name"),
                    "Anonymous"
            );
        }

        if (author instanceof String authorText && !authorText.isBlank()) {
            return authorText.trim();
        }

        return firstNonBlank(
                review.optString("user"),
                review.optString("reviewer_name"),
                "Anonymous"
        );
    }

    private static String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3).trim() + "...";
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nhap thanh pho: ");
        String city = sc.nextLine();
        System.out.println(getHotelsByCity(city));
    }
}
