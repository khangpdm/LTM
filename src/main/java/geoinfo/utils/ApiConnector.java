package geoinfo.utils;

import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ApiConnector {

    public static Document get(String url) throws IOException {
        return Jsoup.connect(url.trim())
                .method(Connection.Method.GET)
                .ignoreContentType(true)
                .execute()
                .parse();
    }
}
