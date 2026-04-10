package geoinfo.client.gui.pages;

import geoinfo.client.gui.utils.Configure;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import geoinfo.server.utils.ValidationUtils;

import java.util.function.Consumer;

public class SearchEnginePage extends BorderPane {

    private TextField txtSearch;
    private ComboBox<String> cbbType;
    private BorderPane pnlContent;
    private TextArea resultArea;
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
        txtSearch.setPromptText("Nhap tu khoa can tim kiem ... ");
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
        cbbType.setStyle(
                "-fx-background-color: black;" +
                "-fx-border-color: #00AEEF;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 4 10;" +
                "-fx-font-size: 12px;"
        );
        cbbType.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: white; -fx-background-color: black;");
            }
        });

        pnlContent = new BorderPane();

        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setStyle(
                "-fx-background-color: -fx-control-inner-background; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-background-radius: 0;"
        );
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
        pnlContent.setCenter(resultArea);

        this.setTop(searchBar);
        this.setStyle("-fx-background-color: white;");
        this.setCenter(pnlContent);
    }

    private void search() {
        search(txtSearch.getText(), cbbType.getValue(), true, null);
    }

    private void setSearchingState(boolean searching) {
        this.searching = searching;
        txtSearch.setDisable(searching);
        cbbType.setDisable(searching);
        searchIcon.setDisable(searching);
    }

    public void setResult(String result) {
        resultArea.setText(result);
    }

    public void clearResult() {
        resultArea.clear();
    }

    public void searchCountryFromMap(String countryName, Consumer<String> onResult) {
        search(countryName, "Country", false, onResult);
    }

    private void search(String keyword, String type, boolean updateMainResult, Consumer<String> onResult) {
        if (searching) {
            if (onResult != null) {
                onResult.accept("Dang co mot yeu cau tim kiem khac.");
            }
            return;
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ").trim();
        String normalizedType = type == null ? "Country" : type;
        String validationMessage = validateKeyword(normalizedKeyword);

        if (validationMessage != null) {
            if (updateMainResult) {
                resultArea.setText(validationMessage);
            }
            if (onResult != null) {
                onResult.accept(validationMessage);
            }
            return;
        }

        txtSearch.setText(normalizedKeyword);
        cbbType.setValue(normalizedType);

        String request = normalizedType.toLowerCase() + ":" + normalizedKeyword;
        String loadingMessage = normalizedType.equals("City")
                ? "Dang tim kiem thanh pho..."
                : "Dang tim kiem quoc gia...";

        setSearchingState(true);
        if (updateMainResult) {
            resultArea.setText(loadingMessage);
        }
        if (onResult != null) {
            onResult.accept(loadingMessage);
        }

        Thread thread = new Thread(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "Loi khi tim kiem: " + ex.getMessage();
            }

            final String finalResponse = response;
            Platform.runLater(() -> {
                if (updateMainResult) {
                    resultArea.setText(finalResponse);
                }
                if (onResult != null) {
                    onResult.accept(finalResponse);
                }
                setSearchingState(false);
            });
        });

        //thread.setDaemon(true);
        thread.setName("SearchThread-" + System.currentTimeMillis());
        thread.start();;
    }

    private String validateKeyword(String keyword) {
        if (keyword.isEmpty()) {
            return "No results found. Try a different search term.";
        }

        if (keyword.length() > 100) {
            return "Tu khoa qua dai.";
        }

        if (!keyword.matches("[\\p{L}\\p{M}0-9 .,'()-]+")) {
            return "Tu khoa chua ky tu khong hop le.";
        }

        return null;
    }
}
