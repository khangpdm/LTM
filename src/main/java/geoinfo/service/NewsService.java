package geoinfo.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLEncoder;

public class NewsService {

    public static String getNewsInfo(String input) {
        try{
            String keyword = input;
            String encoded = URLEncoder.encode(keyword, "UTF8");
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "&hl=en-US&gl=US&ceid=US:en";
            URL url = new URL(rssUrl);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(url.openStream());

            NodeList items = doc.getElementsByTagName("item");

            int limit;
            if(items.getLength() < 5){
                limit = items.getLength();
            } else {
                limit = 5;
            }

            String output = "";
            for(int i = 0; i < limit; i++){
                Element item = (Element) items.item(i);
                String title = item.getElementsByTagName("title").item(0).getTextContent();
                String link = item.getElementsByTagName("link").item(0).getTextContent();
                String pubDate = item.getElementsByTagName("pubDate").item(0).getTextContent();
                output = output + "- " + title + "\n"
                        + " Link: " + link + "\n"
                        + " Ngày: " + pubDate + "\n";
            }

            return output;

        } catch (Exception e){

            return "Lỗi khi lấy tin tức: " + e.getMessage();
        }

    }
}
