package geoinfo.server.utils;

import org.json.JSONObject;
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

import java.io.IOException;
import java.util.Map;

public class ApiConnector {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static Document get(String url) throws IOException {
        return Jsoup.connect(url.trim())
                .method(Connection.Method.GET)
                .ignoreContentType(true)
                .execute()
                .parse();
    }

    public static JSONObject getJson(String url, Map<String, String> headers) throws IOException {
        Connection connection = Jsoup.connect(url.trim())
                .method(Connection.Method.GET)
                .ignoreContentType(true);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.header(entry.getKey(), entry.getValue());
            }
        }

        String body = connection.execute().body();
        return new JSONObject(body);
    }
}
