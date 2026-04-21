package geoinfo.client.gui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class NewsComponent extends VBox {

    public NewsComponent(JSONObject newsItem) {
        super(8);
        getStyleClass().add("news-card");
        setFillWidth(true);
        buildComponent(newsItem);
    }

    private void buildComponent(JSONObject newsItem) {
        if (newsItem == null) {
            return;
        }

        List<String> fieldOrder = List.of("title", "publishedDate", "link");

        for (String key : fieldOrder) {
            if (!newsItem.has(key)) continue;

            String value = newsItem.optString(key, "");
            if (value.isBlank()) continue;

            HBox row = new HBox(12);
            row.setAlignment(Pos.TOP_LEFT);

            Label keyLabel = new Label(formatKey(key));
            keyLabel.getStyleClass().add("news-card-key");
            keyLabel.setMinWidth(180);
            keyLabel.setPrefWidth(180);

            if ("link".equals(key)) {
                Hyperlink link = new Hyperlink(value);
                link.getStyleClass().add("news-card-link");
                link.setWrapText(true);
                link.setOnAction(e -> openUrl(value));
                row.getChildren().addAll(keyLabel, link);
            } else {
                Label valueLabel = new Label(value);
                valueLabel.getStyleClass().add("news-card-value");
                valueLabel.setWrapText(true);
                valueLabel.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(valueLabel, Priority.ALWAYS);
                row.getChildren().addAll(keyLabel, valueLabel);
            }
            getChildren().add(row);
        }
    }

    private String formatKey(String key) {
        if (key == null || key.isBlank()) return "";
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private void openUrl(String url) {
        try {
            if (url != null && !url.isBlank()) {
                String resolved = url;
                if (!resolved.startsWith("http")) {
                    resolved = "https://" + resolved;
                }
                Desktop.getDesktop().browse(new URI(resolved));
            }
        } catch (Exception e) {
            System.err.println("Cannot open link: " + url);
        }
    }
}
