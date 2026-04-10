package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AttractionService {

    private static final String GEONAMES_USERNAME = "tinmi2005";
    private static final int MAX_ROWS = 3;

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
        String url = "http://api.geonames.org/searchJSON?country="
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
        String detailUrl = "http://api.geonames.org/getJSON?geonameId="
                + geonameId
                + "&username=" + GEONAMES_USERNAME;

        Document detailDoc = ApiConnector.get(detailUrl);
        return new JSONObject(detailDoc.text());
    }

    private static JSONObject buildAttractionItem(JSONObject detail) {
        String name = detail.optString("name", "Unknown");
        String asciiName = detail.optString("asciiName", "");
        String adminName1 = detail.optString("adminName1", "");
        String adminName2 = detail.optString("adminName2", "");
        String countryName = detail.optString("countryName", "");
        String featureName = detail.optString("fcodeName", "");
        String lat = detail.optString("lat", "");
        String lng = detail.optString("lng", "");
        String wikipediaUrl = detail.optString("wikipediaURL", "");
        String alternateNames = buildAlternateNames(detail.optJSONArray("alternateNames"));

        return new JSONObject()
                .put("name", name)
                .put("asciiName", asciiName)
                .put("featureType", featureName)
                .put("region", buildRegion(adminName2, adminName1, countryName))
                .put("coordinates", buildCoordinates(lat, lng))
                .put("wikipediaUrl", wikipediaUrl.isBlank() ? "" : "https://" + wikipediaUrl)
                .put("alternateNames", alternateNames);
    }

    private static String buildRegion(String adminName2, String adminName1, String countryName) {
        StringBuilder region = new StringBuilder();
        appendPart(region, adminName2);
        appendPart(region, adminName1);
        appendPart(region, countryName);
        return region.toString();
    }

    private static String buildCoordinates(String lat, String lng) {
        if (lat == null || lat.isBlank() || lng == null || lng.isBlank()) {
            return "";
        }
        return lat + ", " + lng;
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

    private static void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Nhap ma quoc gia: ");
        String input = scanner.nextLine();
        System.out.println(getAttractionInfo(input));
        scanner.close();
    }
}
