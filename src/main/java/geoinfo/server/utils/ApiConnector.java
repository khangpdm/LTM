package geoinfo.server.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ApiConnector {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static Document get(String url) throws IOException {
        return Jsoup.connect(url.trim())
                .method(Connection.Method.GET)
                .ignoreContentType(true)
                .execute()
                .parse();
    }

    public static JSONObject getJson(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .GET();

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("[")) {
            return new JSONObject().put("result", new JSONArray(trimmed));
        }
        return new JSONObject(trimmed);
    }
}
