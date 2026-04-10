package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.MTable;
import geoinfo.client.gui.utils.Configure;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import geoinfo.server.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchEnginePage extends BorderPane {

    private TextField txtSearch;
    private ComboBox<String> cbbType;
    private BorderPane pnlContent;
    private ScrollPane resultScrollPane;
    private VBox resultContainer;
    private ImageView searchIcon;
    private ClientService clientService;
    private boolean searching;

    public SearchEnginePage(ClientService clientService) {
        initComponents();
        buildLayout();
        this.clientService = clientService;
    }

    private void initComponents() {
        txtSearch = new TextField();
        txtSearch.setPromptText("Enter the keyword to search ...");
        txtSearch.setPrefHeight(Consts.SEARCHBAR_ITEM_HEIGHT);
        txtSearch.setStyle(
                "-fx-background-color: black;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #888888;" +
                "-fx-border-color: transparent;" +
                "-fx-background-radius: 12;" +
                "-fx-background-insets: 0;" +
                "-fx-border-width: 0;"
        );
        txtSearch.setOnAction(e -> search());

        searchIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/icons/search_white.png")));
        searchIcon.setFitWidth(Consts.SEARCHBAR_ITEM_HEIGHT - 10);
        searchIcon.setFitHeight(Consts.SEARCHBAR_ITEM_HEIGHT - 10);

        cbbType = new ComboBox<>();
        cbbType.getItems().addAll("Country", "City");
        cbbType.setValue("Country");
        cbbType.setPrefHeight(Consts.SEARCHBAR_ITEM_HEIGHT);
        cbbType.getStyleClass().add("cbb-style");
        cbbType.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: white; -fx-background-color: black;");
            }
        });

        pnlContent = new BorderPane();

        resultContainer = new VBox(12);
        resultContainer.setPadding(new Insets(12));
        resultContainer.setFillWidth(true);
        resultContainer.setStyle("-fx-background-color: white;");

        resultScrollPane = new ScrollPane(resultContainer);
        resultScrollPane.setFitToWidth(true);
        resultScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
    }

    private void buildLayout() {
        searchIcon.setPickOnBounds(true);
        searchIcon.setOnMouseClicked(e -> search());

        HBox searchBox = new HBox(0);
        searchBox.setStyle(
                "-fx-background-color: black;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: #00AEEF;" +
                "-fx-padding: 0 3;" +
                "-fx-background-radius: 14;"
        );
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        StackPane iconWrapper = new StackPane(searchIcon);
        iconWrapper.setMinWidth(Consts.SEARCHBAR_ITEM_HEIGHT);
        iconWrapper.setPrefWidth(Consts.SEARCHBAR_ITEM_HEIGHT);
        searchBox.getChildren().addAll(iconWrapper, txtSearch);

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(0, 15, 0, 15));
        searchBar.getChildren().addAll(searchBox, cbbType);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        BorderPane.setMargin(pnlContent, new Insets(10, 15, 50, 15));
        Label lblContent = new Label("Searched Information Results");
        lblContent.setFont(Configure.FONT_TITLE_SEARCH_CONTENT);
        lblContent.setPadding(new Insets(0, 0, 15, 0));
        pnlContent.setTop(lblContent);
        pnlContent.setCenter(resultScrollPane);

        setTop(searchBar);
        setStyle("-fx-background-color: white;");
        setCenter(pnlContent);
    }

    private void search() {
        if (searching) {
            return;
        }

        String keyword = ValidationUtils.sanitizeInput(txtSearch.getText());
        String type = cbbType.getValue();

        if (ValidationUtils.isEmpty(keyword)) {
            showMessage("No results found. Try a different search term.");
            return;
        }

        if (keyword.length() > 100) {
            showMessage("Tu khoa qua dai.");
            return;
        }

        if (!ValidationUtils.isValidLocationName(keyword)) {
            showMessage("Tu khoa chua ky tu khong hop le.");
            return;
        }

        String request = type.toLowerCase() + ":" + keyword;
        String loadingMessage = type.equals("City")
                ? "Looking for city information..."
                : "Looking for country information...";

        setSearchingState(true);
        showMessage(loadingMessage);

        Thread thread = new Thread(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "!!! ERROR WHEN SEARCHING: " + ex.getMessage();
            }

            String finalResponse = response;
            Platform.runLater(() -> {
                renderResponse(finalResponse);
                setSearchingState(false);
            });
        });

        thread.setName("SearchThread-" + System.currentTimeMillis());
        thread.start();
    }

    private void setSearchingState(boolean searching) {
        this.searching = searching;
        txtSearch.setDisable(searching);
        cbbType.setDisable(searching);
        searchIcon.setDisable(searching);
    }

    public void setResult(String result) {
        renderResponse(result);
    }

    public void clearResult() {
        resultContainer.getChildren().clear();
    }

    private void renderResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            if (!"success".equalsIgnoreCase(json.optString("status"))) {
                showMessage(json.optString("message", response));
                return;
            }
            showJsonAsTables(json);
        } catch (Exception e) {
            showMessage(response);
        }
    }

    private void showJsonAsTables(JSONObject json) {
        resultContainer.getChildren().clear();

        Label header = new Label(buildTitle(json));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111111;");
        resultContainer.getChildren().add(header);

        MTable infoTable = createInfoTable(json);
        resultContainer.getChildren().add(infoTable);

        addMTableArraySection("News", json.optJSONArray("news"));
        addHotelSection("Hotels", json.optJSONArray("hotels"));
        addMTableArraySection("Attractions", json.optJSONArray("attractions"));
    }

    private String buildTitle(JSONObject json) {
        String type = json.optString("type", "result");
        String name = json.optString("name", "Unknown");
        return name + " (" + type + ")";
    }

    private MTable createInfoTable(JSONObject json) {
        return createInfoTable(json, List.of());
    }

    private MTable createInfoTable(JSONObject json, List<String> excludedKeys) {
        MTable table = new MTable();
        List<MTable.RowData> rows = new ArrayList<>();
        for (String key : json.keySet()) {
            if (shouldSkipKey(key) || excludedKeys.contains(key)) {
                continue;
            }
            rows.add(new MTable.RowData(formatKey(key), stringify(json.opt(key))));
        }
        table.setRows(rows);
        return table;
    }

    private boolean shouldSkipKey(String key) {
        return "status".equals(key)
                || "type".equals(key)
                || "news".equals(key)
                || "hotels".equals(key)
                || "attractions".equals(key);
    }

    private void addMTableArraySection(String title, JSONArray array) {
        if (array == null || array.length() == 0) {
            return;
        }

        Label sectionLabel = new Label(title);
        sectionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
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

            sectionBox.getChildren().add(createInfoTable(item));
        }

        resultContainer.getChildren().add(sectionBox);
    }

    private void addHotelSection(String title, JSONArray array) {
        if (array == null || array.length() == 0) {
            return;
        }

        Label sectionLabel = new Label(title);
        sectionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
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

            MTable hotelTable = createInfoTable(hotel, List.of("reviews"));
            sectionBox.getChildren().add(hotelTable);

            JSONArray reviews = hotel.optJSONArray("reviews");
            if (reviews == null || reviews.length() == 0) {
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
                reviewBox.getChildren().add(createInfoTable(review));
            }
            sectionBox.getChildren().add(reviewBox);
        }

        resultContainer.getChildren().add(sectionBox);
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
}
