package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.SearchResultPane;
import geoinfo.client.gui.utils.Configure;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import geoinfo.server.utils.ValidationUtils;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

public class SearchEnginePage extends BorderPane {
    private final ClientService clientService;
    private final SearchResultPane resultPane;

    private TextField txtSearch;
    private ComboBox<String> cbbType;
    private BorderPane pnlContent;
    private ImageView searchIcon;

    public SearchEnginePage(ClientService clientService) {
        this.clientService = clientService;
        this.resultPane = new SearchResultPane(clientService);
        initComponents();
        buildLayout();
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
        txtSearch.setOnAction(event -> search());

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
    }

    private void buildLayout() {
        searchIcon.setPickOnBounds(true);
        searchIcon.setOnMouseClicked(event -> search());

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
        pnlContent.setCenter(resultPane);

        setTop(searchBar);
        setStyle("-fx-background-color: white;");
        setCenter(pnlContent);
    }

    private void search() {
        String originalKeyword = txtSearch.getText();
        String keyword = ValidationUtils.sanitizeInput(originalKeyword);
        String type = cbbType.getValue();

        if (ValidationUtils.isEmpty(keyword)) {
            resultPane.setText("No results found. Try a different search term.");
            return;
        }
        if (keyword.length() > 100) {
            resultPane.setText("Keyword is too long.");
            return;
        }
        if (!ValidationUtils.isValidLocationName(keyword)) {
            resultPane.setText("Keyword contains illegal characters.");
            return;
        }

        if (!keyword.equals(originalKeyword)) {
            txtSearch.setText(keyword);
        }
        txtSearch.requestFocus();
        txtSearch.positionCaret(keyword.length());
        String request = type.toLowerCase() + ":" + keyword;
        String loadingMessage = type.equals("City")
                ? "Looking for city information..."
                : "Looking for country information...";
        resultPane.search(request, loadingMessage);
    }

    public void setResult(String result) {
        resultPane.setText(result);
    }

    public void clearResult() {
        resultPane.clear();
    }

    public SearchResultPane createResultPane() {
        return new SearchResultPane(clientService);
    }
}
