package geoinfo.client.gui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AttractionAndHotelComponent extends VBox {
    private final double imageWidth;
    private final double imageHeight;

    private final HBox topRow = new HBox(16);
    private final VBox rowsBox = new VBox(16);
    private final VBox reviewsSection = new VBox(8);
    private final VBox reviewsBox = new VBox(8);

    public AttractionAndHotelComponent(double imageWidth, double imageHeight) {
        super(10);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        getStyleClass().add("ah-card");
        setFillWidth(true);

        topRow.setAlignment(Pos.TOP_LEFT);
        topRow.setFillHeight(true);

        rowsBox.setFillWidth(true);
        HBox.setHgrow(rowsBox, Priority.ALWAYS);

        reviewsSection.getStyleClass().add("ah-reviews-section");
        Label title = new Label("Reviews");
        title.getStyleClass().add("ah-reviews-title");
        reviewsSection.getChildren().addAll(title, reviewsBox);
        reviewsSection.setVisible(false);
        reviewsSection.setManaged(false);

        getChildren().addAll(topRow, reviewsSection);
    }

    public static AttractionAndHotelComponent forAttraction(JSONObject attraction) {
        AttractionAndHotelComponent component = new AttractionAndHotelComponent(240, 240);
        component.setMainInfo(attraction, List.of("imageUrl"), "imageUrl");
        component.setReviews(null);
        return component;
    }

    public static AttractionAndHotelComponent forHotel(JSONObject hotel) {
        AttractionAndHotelComponent component = new AttractionAndHotelComponent(280, 280);
        component.setMainInfo(hotel, List.of("reviews", "imageUrl"), "imageUrl");
        component.setReviews(hotel == null ? null : hotel.optJSONArray("reviews"));
        return component;
    }

    private void setMainInfo(JSONObject json, Collection<String> excludedKeys, String imageKey) {
        topRow.getChildren().clear();
        rowsBox.getChildren().clear();

        if (json == null) {
            return;
        }

        String imageUrl = json.optString(imageKey, "").trim();
        Node imageNode = createImageRight(imageUrl);
        boolean showImage = imageNode != null;

        List<String> keys = new ArrayList<>(json.keySet());
        keys.sort(Comparator.comparing(String::toLowerCase));

        List<KeyValue> rows = new ArrayList<>();
        for (String key : keys) {
            if (excludedKeys != null && excludedKeys.contains(key)) {
                if (!(key.equals(imageKey) && !showImage)) {
                    continue;
                }
            }
            rows.add(new KeyValue(formatKey(key), stringify(json.opt(key))));
        }

        if (!rows.isEmpty()) {
            for (KeyValue row : rows) {
                rowsBox.getChildren().add(createInfoRow(row.key(), row.value()));
            }
        }

        if (imageNode == null) {
            topRow.getChildren().add(rowsBox);
        } else {
            topRow.getChildren().addAll(rowsBox, imageNode);
        }
    }

    private void setReviews(JSONArray reviews) {
        reviewsBox.getChildren().clear();

        if (reviews == null || reviews.isEmpty()) {
            reviewsSection.setVisible(false);
            reviewsSection.setManaged(false);
            return;
        }

        for (int i = 0; i < reviews.length(); i++) {
            JSONObject review = reviews.optJSONObject(i);
            if (review == null) {
                continue;
            }
            Node card = createReviewCard(review);
            if (card != null) {
                reviewsBox.getChildren().add(card);
            }
        }

        boolean hasReviews = !reviewsBox.getChildren().isEmpty();
        reviewsSection.setVisible(hasReviews);
        reviewsSection.setManaged(hasReviews);
    }

    private Node createInfoRow(String field, String value) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        Label k = new Label(safeText(field));
        k.getStyleClass().add("ah-key");
        k.setMinWidth(180);
        k.setPrefWidth(180);
        k.setWrapText(true);

        String text = safeText(value);
        Label v = new Label(text);
        v.getStyleClass().add("ah-value");
        if (isUrl(text)) {
            v.getStyleClass().add("ah-link");
        }
        v.setWrapText(true);
        v.setMaxWidth(Double.MAX_VALUE);
        addUrlSupport(v, text);

        HBox.setHgrow(v, Priority.ALWAYS);
        row.getChildren().addAll(k, v);
        return row;
    }

    private Node createImageRight(String url) {
        final ImageView view = new ImageView();
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setFitWidth(this.imageWidth);
        view.setFitHeight(this.imageHeight);

        String placeholderUrl = getPlaceholderUrl();
        if (placeholderUrl != null) {
            view.setImage(new Image(placeholderUrl, true));
        }

        String initialUrl = (url == null || url.isBlank()) ? null : url.trim();

        if (initialUrl != null) {
            new Thread(() -> {
                String finalImageUrl = initialUrl;
                if (finalImageUrl.startsWith("//")) {
                    finalImageUrl = "https:" + finalImageUrl;
                } else if (!finalImageUrl.toLowerCase().startsWith("http")) {
                    finalImageUrl = "https://" + finalImageUrl;
                }

                InputStream inputStream = null;
                try {
                    URL imageUrl = new URL(finalImageUrl);
                    HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                    // Set a browser-like User-Agent to prevent 403 Forbidden errors
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
                    inputStream = connection.getInputStream();
                    final Image image = new Image(inputStream);

                    Platform.runLater(() -> {
                        view.setImage(image);
                    });

                } catch (Exception e) {
                    System.err.println("Failed to manually load image with User-Agent: " + finalImageUrl);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }).start();
        }

        VBox wrapper = new VBox(view);
        wrapper.setAlignment(Pos.TOP_RIGHT);
        wrapper.setPadding(new Insets(0));
        wrapper.setMinWidth(this.imageWidth);
        wrapper.setPrefWidth(this.imageWidth);
        wrapper.setMaxWidth(this.imageWidth);
        return wrapper;
    }

    private String getPlaceholderUrl() {
        try {
            return getClass().getResource("/images/logo/world_map.png").toExternalForm();
        } catch (Exception e) {
            return null;
        }
    }

    private Node createReviewCard(JSONObject review) {
        if (review == null) {
            return null;
        }

        List<String> keys = new ArrayList<>(review.keySet());
        keys.sort(Comparator.comparing(String::toLowerCase));

        VBox box = new VBox(8);
        box.getStyleClass().add("ah-review-card");
        box.setFillWidth(true);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = stringify(review.opt(key));
            if (value.isBlank()) {
                continue;
            }

            HBox row = new HBox(12);
            row.setAlignment(Pos.TOP_LEFT);

            Label k = new Label(formatKey(key));
            k.getStyleClass().add("ah-review-key");
            k.setMinWidth(110);
            k.setPrefWidth(110);

            Label v = new Label(value);
            v.getStyleClass().add("ah-review-value");
            if (isUrl(value)) {
                v.getStyleClass().add("ah-link");
            }
            v.setMaxWidth(Double.MAX_VALUE);
            addUrlSupport(v, value);

            HBox.setHgrow(v, Priority.ALWAYS);
            row.getChildren().addAll(k, v);
            box.getChildren().add(row);
        }

        if (box.getChildren().isEmpty()) {
            return null;
        }

        return box.getChildren().isEmpty() ? null : box;
    }

    private void addUrlSupport(Node node, String text) {
        if (!isUrl(text)) {
            return;
        }
        node.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.isControlDown()) {
                openUrl(text);
                event.consume();
            }
        });
    }

    private boolean isUrl(String text) {
        if (text == null || text.isBlank()) return false;
        String lowerText = text.toLowerCase().trim();
        return lowerText.startsWith("http://") || lowerText.startsWith("https://") || lowerText.startsWith("www.");
    }

    private void openUrl(String url) {
        try {
            String u = url;
            if (!u.startsWith("http")) {
                u = "https://" + u;
            }
            Desktop.getDesktop().browse(new URI(u));
        } catch (Exception e) {
            System.err.println("Cannot open link: " + url);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof JSONArray array) {
            return array.toString();
        }
        if (value instanceof JSONObject object) {
            return object.toString();
        }
        return String.valueOf(value);
    }

    private record KeyValue(String key, String value) {
    }
}
