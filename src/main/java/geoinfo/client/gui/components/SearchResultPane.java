package geoinfo.client.gui.components;

import geoinfo.client.network.ClientService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class SearchResultPane extends VBox {
    private static final ExecutorService SEARCH_EXECUTOR =
            Executors.newCachedThreadPool(new SearchThreadFactory());

    private final AtomicLong latestRequestId = new AtomicLong();
    private final ClientService clientService;

    private final ImageView flagPreview;
    private final Label titleLabel;
    private final Button moreInfoButton;
    private final VBox resultContainer;
    private final ScrollPane resultScrollPane;

    private String pendingMoreInfoRequest;
    private String loadedMoreInfoRequest;

    // Thêm biến lưu cache more info
    private JSONObject cachedMoreInfo;
    private boolean isLoadingMoreInfo;

    public SearchResultPane(ClientService clientService) {
        this.clientService = clientService;

        flagPreview = new ImageView();
        flagPreview.setFitHeight(36);
        flagPreview.setFitWidth(54);
        flagPreview.setPreserveRatio(true);
        flagPreview.setVisible(false);
        flagPreview.setManaged(false);

        titleLabel = new Label("");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111111;");
        titleLabel.setVisible(false);
        titleLabel.setManaged(false);

        moreInfoButton = new Button("Them thong tin");
        moreInfoButton.setVisible(false);
        moreInfoButton.setManaged(false);
        moreInfoButton.setDisable(true);
        moreInfoButton.setOnAction(event -> fetchMoreInfo());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerBar = new HBox(12, flagPreview, titleLabel, spacer, moreInfoButton);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        resultContainer = new VBox(12);
        resultContainer.setPadding(new Insets(12));
        resultContainer.setFillWidth(true);
        resultContainer.setStyle("-fx-background-color: white;");

        resultScrollPane = new ScrollPane(resultContainer);
        resultScrollPane.setFitToWidth(true);
        resultScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");

        setSpacing(12);
        getChildren().addAll(headerBar, resultScrollPane);
        VBox.setVgrow(resultScrollPane, Priority.ALWAYS);
    }

    public void setText(String text) {
        clearSupplementaryUi();
        showMessage(text);
    }

    public void clear() {
        clearSupplementaryUi();
        resultContainer.getChildren().clear();
    }

    public void search(String request, String loadingMessage) {
        long requestId = latestRequestId.incrementAndGet();
        pendingMoreInfoRequest = null;
        loadedMoreInfoRequest = null;
        clearSupplementaryUi();
        showMessage(loadingMessage);

        SEARCH_EXECUTOR.submit(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "!!! ERROR WHEN SEARCHING: " + ex.getMessage();
            }

            String finalResponse = response;
            Platform.runLater(() -> {
                if (requestId != latestRequestId.get()) {
                    return;
                }
                renderResponse(finalResponse, false);
            });
        });
    }

    private void fetchMoreInfo() {
        String request = pendingMoreInfoRequest;
        if (request == null || request.isBlank()) {
            return;
        }
        if (request.equals(loadedMoreInfoRequest)) {
            return;
        }

        // === THÊM: Dùng cache nếu đã có ===
        if (cachedMoreInfo != null) {
            renderResponse(cachedMoreInfo.toString(), true);
            loadedMoreInfoRequest = request;
            moreInfoButton.setText("Da them thong tin");
            moreInfoButton.setDisable(true);
            return;
        }

        // === THÊM: Nếu đang loading thì chờ ===
        if (isLoadingMoreInfo) {
            moreInfoButton.setDisable(true);
            moreInfoButton.setText("Dang tai...");

            // Poll chờ cache sẵn sàng
            SEARCH_EXECUTOR.submit(() -> {
                while (isLoadingMoreInfo) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                Platform.runLater(this::fetchMoreInfo);
            });
            return;
        }

        // Fallback: gọi API như cũ nếu không có cache
        long requestId = latestRequestId.get();
        moreInfoButton.setDisable(true);
        moreInfoButton.setText("Dang tai them...");

        SEARCH_EXECUTOR.submit(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "Loi khi tai them thong tin: " + ex.getMessage();
            }

            String finalResponse = response;
            Platform.runLater(() -> {
                if (requestId != latestRequestId.get()) {
                    return;
                }
                renderResponse(finalResponse, true);
                loadedMoreInfoRequest = request;
                moreInfoButton.setText("Da them thong tin");
                moreInfoButton.setDisable(true);
            });
        });
    }

    private void renderResponse(String response, boolean appendMode) {
        try {
            JSONObject json = new JSONObject(response);
            if (!"success".equalsIgnoreCase(json.optString("status"))) {
                if (!appendMode) {
                    clearSupplementaryUi();
                    showMessage(json.optString("message", response));
                } else {
                    appendMessage(json.optString("message", response));
                }
                return;
            }

            if (!appendMode) {
                applyHeader(json);
                showJsonAsTables(json);
            } else {
                appendAdditionalSections(json);
            }
        } catch (Exception e) {
            if (!appendMode) {
                clearSupplementaryUi();
                showMessage(response);
            } else {
                appendMessage(response);
            }
        }
    }

    private void applyHeader(JSONObject json) {
        String flagUrl = json.optString("flagUrl", "").trim();
        if (flagUrl.isBlank()) {
            flagPreview.setImage(null);
            flagPreview.setVisible(false);
            flagPreview.setManaged(false);
        } else {
            flagPreview.setImage(new Image(flagUrl, true));
            flagPreview.setVisible(true);
            flagPreview.setManaged(true);
        }

        String title = buildTitle(json);
        titleLabel.setText(title);
        titleLabel.setVisible(!title.isBlank());
        titleLabel.setManaged(!title.isBlank());

        pendingMoreInfoRequest = json.optString("moreInfoRequest", "").trim();
        loadedMoreInfoRequest = null;
        cachedMoreInfo = null;           // === THÊM: reset cache ===
        isLoadingMoreInfo = false;      // === THÊM: reset loading flag ===
        if (pendingMoreInfoRequest.isBlank()) {
            moreInfoButton.setVisible(false);
            moreInfoButton.setManaged(false);
            moreInfoButton.setDisable(true);
            moreInfoButton.setText("Them thong tin");
        } else {
            moreInfoButton.setVisible(true);
            moreInfoButton.setManaged(true);
            moreInfoButton.setDisable(false);
            moreInfoButton.setText(json.optString("moreInfoLabel", "Them thong tin"));
            // === THÊM: Prefetch more info trong background ===
            prefetchMoreInfo(pendingMoreInfoRequest);

        }
    }

    private void prefetchMoreInfo(String request) {
        if (request == null || request.isBlank()) {
            return;
        }

        long requestId = latestRequestId.get();
        isLoadingMoreInfo = true;

        SEARCH_EXECUTOR.submit(() -> {
            JSONObject result = null;
            try {
                String response = clientService.sendRequest(request);
                JSONObject json = new JSONObject(response);
                if ("success".equalsIgnoreCase(json.optString("status"))) {
                    result = json;
                }
            } catch (Exception ex) {
                // Ignore error, user can retry manually
            }

            JSONObject finalResult = result;
            Platform.runLater(() -> {
                // Chỉ cache nếu vẫn đang ở cùng request
                if (requestId == latestRequestId.get()) {
                    cachedMoreInfo = finalResult;
                    isLoadingMoreInfo = false;
                }
            });
        });
    }

    private void showJsonAsTables(JSONObject json) {
        resultContainer.getChildren().clear();

        MTable infoTable = createInfoTable(json);
        if (infoTable != null) {
            resultContainer.getChildren().add(infoTable);
        }

        addArraySection("News", json.optJSONArray("news"));
        addHotelSection("Hotels", json.optJSONArray("hotels"));
        addArraySection("Attractions", json.optJSONArray("attractions"));
    }

    private void appendAdditionalSections(JSONObject json) {
        addArraySection("News", json.optJSONArray("news"));
        addHotelSection("Hotels", json.optJSONArray("hotels"));
        addArraySection("Attractions", json.optJSONArray("attractions"));

        String message = json.optString("message", "").trim();
        if (!message.isBlank()) {
            appendMessage(message);
        }
    }

    private MTable createInfoTable(JSONObject json) {
        List<MTable.RowData> rows = new ArrayList<>();
        for (String key : json.keySet()) {
            if (shouldSkipKey(key)) {
                continue;
            }
            rows.add(new MTable.RowData(formatKey(key), stringify(json.opt(key))));
        }

        if (rows.isEmpty()) {
            return null;
        }

        MTable table = new MTable();
        table.setRows(rows);
        return table;
    }

    private void addArraySection(String title, JSONArray array) {
        if (array == null || array.isEmpty()) {
            return;
        }

        Label sectionLabel = createSectionLabel(title);
        resultContainer.getChildren().add(sectionLabel);

        VBox sectionBox = new VBox(10);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }

            if (array.length() > 1) {
                Label itemLabel = new Label(title + " " + (i + 1));
                itemLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
                sectionBox.getChildren().add(itemLabel);
            }

            MTable itemTable = createInfoTable(item);
            if (itemTable != null) {
                sectionBox.getChildren().add(itemTable);
            }
        }

        if (!sectionBox.getChildren().isEmpty()) {
            resultContainer.getChildren().add(sectionBox);
        }
    }

    private void addHotelSection(String title, JSONArray array) {
        if (array == null || array.isEmpty()) {
            return;
        }

        Label sectionLabel = createSectionLabel(title);
        resultContainer.getChildren().add(sectionLabel);

        VBox sectionBox = new VBox(10);
        for (int i = 0; i < array.length(); i++) {
            JSONObject hotel = array.optJSONObject(i);
            if (hotel == null) {
                continue;
            }

            Label itemLabel = new Label("Hotel " + (i + 1));
            itemLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
            sectionBox.getChildren().add(itemLabel);

            MTable hotelTable = createInfoTableExcluding(hotel, List.of("reviews"));
            if (hotelTable != null) {
                sectionBox.getChildren().add(hotelTable);
            }

            JSONArray reviews = hotel.optJSONArray("reviews");
            if (reviews == null || reviews.isEmpty()) {
                continue;
            }

            Label reviewLabel = new Label("Reviews");
            reviewLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4b5563;");
            sectionBox.getChildren().add(reviewLabel);

            VBox reviewBox = new VBox(8);
            for (int reviewIndex = 0; reviewIndex < reviews.length(); reviewIndex++) {
                JSONObject review = reviews.optJSONObject(reviewIndex);
                if (review == null) {
                    continue;
                }

                MTable reviewTable = createInfoTable(review);
                if (reviewTable != null) {
                    reviewBox.getChildren().add(reviewTable);
                }
            }
            if (!reviewBox.getChildren().isEmpty()) {
                sectionBox.getChildren().add(reviewBox);
            }
        }

        if (!sectionBox.getChildren().isEmpty()) {
            resultContainer.getChildren().add(sectionBox);
        }
    }

    private MTable createInfoTableExcluding(JSONObject json, List<String> excludedKeys) {
        List<MTable.RowData> rows = new ArrayList<>();
        for (String key : json.keySet()) {
            if (shouldSkipKey(key) || excludedKeys.contains(key)) {
                continue;
            }
            rows.add(new MTable.RowData(formatKey(key), stringify(json.opt(key))));
        }

        if (rows.isEmpty()) {
            return null;
        }

        MTable table = new MTable();
        table.setRows(rows);
        return table;
    }

    private boolean shouldSkipKey(String key) {
        return "status".equals(key)
                || "type".equals(key)
                || "name".equals(key)
                || "news".equals(key)
                || "hotels".equals(key)
                || "attractions".equals(key)
                || "flagUrl".equals(key)
                || "moreInfoRequest".equals(key)
                || "moreInfoLabel".equals(key)
                || "message".equals(key);
    }

    private Label createSectionLabel(String title) {
        Label sectionLabel = new Label(title);
        sectionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        return sectionLabel;
    }

    private String buildTitle(JSONObject json) {
        String name = json.optString("name", "").trim();
        if (name.isBlank()) {
            return "";
        }

        String type = json.optString("type", "").trim();
        if (type.isBlank()) {
            return name;
        }
        return name + " (" + type + ")";
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

    private void clearSupplementaryUi() {
        flagPreview.setImage(null);
        flagPreview.setVisible(false);
        flagPreview.setManaged(false);
        titleLabel.setText("");
        titleLabel.setVisible(false);
        titleLabel.setManaged(false);
        moreInfoButton.setVisible(false);
        moreInfoButton.setManaged(false);
        moreInfoButton.setDisable(true);
        moreInfoButton.setText("Them thong tin");

        cachedMoreInfo = null;
        isLoadingMoreInfo = false;
    }

    private void showMessage(String message) {
        resultContainer.getChildren().clear();

        Label messageLabel = new Label(message == null ? "" : message.trim());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setAlignment(Pos.CENTER_LEFT);
        messageLabel.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 16;" +
                        "-fx-text-fill: #111827;"
        );
        resultContainer.getChildren().add(messageLabel);
    }

    private void appendMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Label messageLabel = new Label(message.trim());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setAlignment(Pos.CENTER_LEFT);
        messageLabel.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 16;" +
                        "-fx-text-fill: #111827;"
        );
        resultContainer.getChildren().add(messageLabel);
    }

    private static class SearchThreadFactory implements ThreadFactory {
        private final AtomicLong sequence = new AtomicLong(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "SearchThread-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
