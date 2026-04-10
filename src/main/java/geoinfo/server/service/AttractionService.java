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
        if (input == null || input.isBlank()) {
            return "Không tìm thấy mã quốc gia.";
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
                return "Không tìm thấy địa điểm du lịch cho quốc gia: " + countryCode;
            }

            StringBuilder result = new StringBuilder();
            result.append("ĐỊA ĐIỂM DU LỊCH NỔI BẬT\n");
            result.append("-----------------------------------------------\n");

            for (int i = 0; i < geonames.length(); i++) {
                JSONObject item = geonames.getJSONObject(i);
                int geonameId = item.optInt("geonameId", 0);

                if (geonameId == 0) {
                    continue;
                }

                JSONObject detail = getAttractionDetail(geonameId);
                result.append(formatAttractionDetail(i + 1, detail));
                if (i < geonames.length() - 1) {
                    result.append("\n");
                }
            }
            return result.toString();

        } catch (IOException e) {
            return "Lỗi khi gọi API attraction: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } catch (Exception e) {
            return "Lỗi khi xử lý dữ liệu attraction: " + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static JSONObject getAttractionDetail(int geonameId) throws IOException {
        String detailUrl = "http://api.geonames.org/getJSON?geonameId="
                + geonameId
                + "&username=" + GEONAMES_USERNAME;

        Document detailDoc = ApiConnector.get(detailUrl);
        return new JSONObject(detailDoc.text());
    }

    private static String formatAttractionDetail(int index, JSONObject detail) {
        String name = detail.optString("name", "Không rõ tên");
        String asciiName = detail.optString("asciiName", "");
        String adminName1 = detail.optString("adminName1", "Không rõ tỉnh/thành");
        String adminName2 = detail.optString("adminName2", "Không rõ quận/huyện");
        String countryName = detail.optString("countryName", "Không rõ quốc gia");
        String fcodeName = detail.optString("fcodeName", "Không rõ loại");
        String lat = detail.optString("lat", "");
        String lng = detail.optString("lng", "");
        String wikipediaURL = detail.optString("wikipediaURL", "");
        String alternateNames = buildAlternateNames(detail.optJSONArray("alternateNames"));

        StringBuilder sb = new StringBuilder();
        sb.append(index).append(". ").append(name).append("\n");

        if (!asciiName.isBlank() && !asciiName.equalsIgnoreCase(name)) {
            sb.append("   Tên ASCII : ").append(asciiName).append("\n");
        }
        sb.append("   Loại      : ").append(fcodeName).append("\n");
        sb.append("   Khu vực   : ").append(adminName2).append(", ")
                .append(adminName1).append(", ")
                .append(countryName).append("\n");
        sb.append("   Tọa độ    : ").append(lat).append(", ").append(lng).append("\n");

        if (!wikipediaURL.isBlank()) {
            sb.append("   Wikipedia : https://").append(wikipediaURL).append("\n");
        }

        if (!alternateNames.isBlank()) {
            sb.append("   Tên khác  : ").append(alternateNames).append("\n");
        }

        sb.append("   ").append("-".repeat(55)).append("\n");
        return sb.toString();
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

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Nhập mã quốc gia: ");
        String input = scanner.nextLine();

        String result = getAttractionInfo(input);
        System.out.println(result);

        scanner.close();
    }
}
