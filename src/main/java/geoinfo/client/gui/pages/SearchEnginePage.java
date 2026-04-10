package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.SearchResultPane;
import geoinfo.client.gui.utils.Configure;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
        txtSearch.setOnAction(event -> search());

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

        Label lblContent = new Label("Searched Information Results");
        lblContent.setFont(Configure.FONT_TITLE_SEARCH_CONTENT);
        VBox contentHeader = new VBox(lblContent);
        contentHeader.setPadding(new Insets(0, 0, 15, 0));

        BorderPane.setMargin(pnlContent, new Insets(10, 15, 50, 15));
        pnlContent.setTop(contentHeader);
        pnlContent.setCenter(resultPane);

        setTop(searchBar);
        setStyle("-fx-background-color: white;");
        setCenter(pnlContent);
    }

    private void search() {
        String normalizedKeyword = txtSearch.getText() == null
                ? ""
                : txtSearch.getText().trim().replaceAll("\\s+", " ").trim();
        String normalizedType = cbbType.getValue() == null ? "Country" : cbbType.getValue();
        String validationMessage = validateKeyword(normalizedKeyword);

        if (validationMessage != null) {
            resultPane.setText(validationMessage);
            return;
        }

        txtSearch.setText(normalizedKeyword);
        cbbType.setValue(normalizedType);

        String request = normalizedType.toLowerCase() + ":" + normalizedKeyword;
        String loadingMessage = normalizedType.equals("City")
                ? "Dang tim kiem thanh pho..."
                : "Dang tim kiem quoc gia...";
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
