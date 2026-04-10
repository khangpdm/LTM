package geoinfo.server.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NewsService {

    public static String getNewsInfo(String input) {
        try {
            String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8);
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "&hl=en-US&gl=US&ceid=US:en";
            URL url = new URL(rssUrl);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(url.openStream());

            NodeList items = doc.getElementsByTagName("item");
            int limit = Math.min(items.getLength(), 5);

            if (limit == 0) {
                return "Không có tin tức liên quan.";
            }

            StringBuilder output = new StringBuilder();
            for (int i = 0; i < limit; i++) {
                Element item = (Element) items.item(i);
                Element titleEl = (Element) item.getElementsByTagName("title").item(0);
                Element linkEl = (Element) item.getElementsByTagName("link").item(0);
                Element pubDateEl = (Element) item.getElementsByTagName("pubDate").item(0);

                if (titleEl == null || linkEl == null || pubDateEl == null) {
                    continue;  // Skip item này
                }

                String title = item.getElementsByTagName("title").item(0).getTextContent();
                String link = item.getElementsByTagName("link").item(0).getTextContent();
                String pubDate = item.getElementsByTagName("pubDate").item(0).getTextContent();

                output.append("- ").append(title).append("\n")
                        .append("  Link: ").append(link).append("\n")
                        .append("  Ngày: ").append(pubDate).append("\n");
            }

            return output.toString();
        } catch (Exception e) {
            return "Lỗi khi lấy tin tức: " + e.getMessage();
        }
    }
}
