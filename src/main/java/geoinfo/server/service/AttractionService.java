package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AttractionService {

    private static final String GEONAMES_USERNAME = "tinmi2005";
    private static final int MAX_ROWS = 3;

    private static final String GEONAMES_SEARCH_API_URL = "http://api.geonames.org/searchJSON";
    private static final String GEONAMES_GET_API_URL = "http://api.geonames.org/getJSON";
    private static final String HTTPS_PREFIX = "https://";
    private static final String WIKIPEDIA_REST_SUMMARY_PATH = "/api/rest_v1/page/summary/";

    public static String getAttractionInfo(String input) {
        JSONObject response = new JSONObject()
                .put("status", "success")
                .put("type", "attractions")
                .put("countryCode", input == null ? "" : input.trim().toUpperCase())
                .put("items", new JSONArray());

        if (input == null || input.isBlank()) {
            return response.put("status", "error")
                    .put("message", "Country code is required.")
                    .toString(2);
        }

        String countryCode = input.trim().toUpperCase();
        String url = GEONAMES_SEARCH_API_URL + "?country="
                + URLEncoder.encode(countryCode, StandardCharsets.UTF_8)
                + "&featureClass=T"
                + "&maxRows=" + MAX_ROWS
                + "&username=" + GEONAMES_USERNAME;

        try {
            Document doc = ApiConnector.get(url);
            JSONObject json = new JSONObject(doc.text());
            JSONArray geonames = json.optJSONArray("geonames");

            if (geonames == null || geonames.isEmpty()) {
                return response.put("message", "No attractions found for the given country code.")
                        .toString(2);
            }

            JSONArray items = new JSONArray();
            for (int i = 0; i < geonames.length(); i++) {
                JSONObject item = geonames.optJSONObject(i);
                if (item == null) {
                    continue;
                }

                int geonameId = item.optInt("geonameId", 0);
                if (geonameId == 0) {
                    continue;
                }

                JSONObject detail = getAttractionDetail(geonameId);
                items.put(buildAttractionItem(detail));
            }

            response.put("items", items);
            if (items.isEmpty()) {
                response.put("message", "No valid attraction details found.");
            }

            return response.toString(2);
        } catch (IOException e) {
            return response.put("status", "error")
                    .put("message", "Failed to call attraction API: " + e.getMessage())
                    .toString(2);
        } catch (Exception e) {
            return response.put("status", "error")
                    .put("message", "Failed to process attraction data: " + e.getMessage())
                    .toString(2);
        }
    }

    private static JSONObject getAttractionDetail(int geonameId) throws IOException {
        String detailUrl = GEONAMES_GET_API_URL + "?geonameId="
                + geonameId
                + "&username=" + GEONAMES_USERNAME;

        Document detailDoc = ApiConnector.get(detailUrl);
        return new JSONObject(detailDoc.text());
    }

    private static JSONObject buildAttractionItem(JSONObject detail) {
        String name = detail.optString("name", "Unknown");
        String asciiName = detail.optString("asciiName", "");
        String featureName = detail.optString("fcodeName", "");
        double lat = detail.optDouble("lat", Double.NaN);
        double lng = detail.optDouble("lng", Double.NaN);
        String wikipediaUrl = detail.optString("wikipediaURL", "");
        String thumbnailImg = detail.optString("thumbnailImg", "");
        String imageUrl = thumbnailImg == null ? "" : thumbnailImg.trim();
        if (imageUrl.isBlank()) {
            imageUrl = fetchWikipediaThumbnail(wikipediaUrl);
        }
        String alternateNames = buildAlternateNames(detail.optJSONArray("alternateNames"));

        JSONObject result = new JSONObject()
                .put("name", name)
                .put("asciiName", asciiName)
                .put("featureType", featureName)
                .put("coordinates", buildCoordinates(lat, lng))
                .put("imageUrl", imageUrl)
                .put("wikipediaUrl", wikipediaUrl.isBlank() ? "" : HTTPS_PREFIX + wikipediaUrl)
                .put("alternateNames", alternateNames);

        if (imageUrl.isBlank()) {
            result.remove("imageUrl");
        }

        return result;
    }

    private static String fetchWikipediaThumbnail(String wikipediaUrl) {
        try {
            if (wikipediaUrl == null || wikipediaUrl.isBlank()) {
                return "";
            }

            String fullUrl = wikipediaUrl.startsWith("http") ? wikipediaUrl.trim() : (HTTPS_PREFIX + wikipediaUrl.trim());
            URI uri = URI.create(fullUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || host.isBlank() || path == null || !path.contains("/wiki/")) {
                return "";
            }

            String title = path.substring(path.indexOf("/wiki/") + "/wiki/".length());
            if (title.isBlank()) {
                return "";
            }

            // Title is usually URL-encoded already; decode once, then encode as a path segment.
            title = URLDecoder.decode(title, StandardCharsets.UTF_8);
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20");

            String summaryUrl = HTTPS_PREFIX + host + WIKIPEDIA_REST_SUMMARY_PATH + encodedTitle;
            Document doc = ApiConnector.get(summaryUrl);
            if (doc == null) {
                return "";
            }

            String body = doc.text();
            if (body == null || body.isBlank()) {
                return "";
            }

            JSONObject json = new JSONObject(body);
            JSONObject thumbnail = json.optJSONObject("thumbnail");
            if (thumbnail != null) {
                String src = thumbnail.optString("source", "").trim();
                if (!src.isBlank()) {
                    return src;
                }
            }

            JSONObject original = json.optJSONObject("originalimage");
            if (original != null) {
                return original.optString("source", "").trim();
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static Object buildCoordinates(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            return "";
        }
        return new JSONArray().put(lng).put(lat);
    }

    private static String buildAlternateNames(JSONArray alternateNames) {
        if (alternateNames == null || alternateNames.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < alternateNames.length() && shown < 4; i++) {
            JSONObject alt = alternateNames.optJSONObject(i);
            if (alt == null) {
                continue;
            }

            String altName = alt.optString("name", "").trim();
            String lang = alt.optString("lang", "").trim();
            if (altName.isBlank() || "link".equalsIgnoreCase(lang)) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(altName);
            shown++;
        }

        return result.toString();
    }

}
