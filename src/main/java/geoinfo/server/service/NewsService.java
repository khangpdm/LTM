package geoinfo.server.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NewsService {
    private static final String GOOGLE_NEWS_RSS_SEARCH_URL = "https://news.google.com/rss/search";

    public static String getNewsInfo(String input) {
        JSONObject response = new JSONObject()
                .put("status", "success")
                .put("type", "news")
                .put("query", input == null ? "" : input.trim())
                .put("items", new JSONArray());

        try {
            String encoded = URLEncoder.encode(input == null ? "" : input, StandardCharsets.UTF_8);
            String rssUrl = GOOGLE_NEWS_RSS_SEARCH_URL
                    + "?q=" + encoded
                    + "&hl=en-US&gl=US&ceid=US:en";
            URL url = new URL(rssUrl);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(url.openStream());

            NodeList items = doc.getElementsByTagName("item");
            JSONArray newsItems = new JSONArray();
            int limit = Math.min(items.getLength(), 5);

            for (int i = 0; i < limit; i++) {
                Element item = (Element) items.item(i);
                Element titleEl = (Element) item.getElementsByTagName("title").item(0);
                Element linkEl = (Element) item.getElementsByTagName("link").item(0);
                Element pubDateEl = (Element) item.getElementsByTagName("pubDate").item(0);

                if (titleEl == null || linkEl == null || pubDateEl == null) {
                    continue;
                }

                newsItems.put(new JSONObject()
                        .put("title", titleEl.getTextContent().trim())
                        .put("link", linkEl.getTextContent().trim())
                        .put("publishedDate", pubDateEl.getTextContent().trim()));
            }

            response.put("items", newsItems);
            if (newsItems.isEmpty()) {
                response.put("message", "No related news found.");
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fetch news: " + e.getMessage());
        }

        return response.toString(2);
    }
}
