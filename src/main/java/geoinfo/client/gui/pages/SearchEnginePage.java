package geoinfo.client.gui.pages;

import geoinfo.client.gui.utils.*;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;


public class SearchEnginePage extends BorderPane {

    private TextField txtSearch;
    private ComboBox<String> cbbType;
    private TextArea resultArea;
    private ImageView searchIcon;

    public SearchEnginePage() {
        initComponents();
        buildLayout();
    }

    private void initComponents() {
        txtSearch = new TextField();
        txtSearch.setPromptText("Nhập từ khóa cần tìm kiếm ... ");
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

        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPromptText("Search result will be displayed here...");
    }

    private void buildLayout() {
        // ================ SEARCH BAR ===============
        // ===== Search Icon =====
        searchIcon.setPickOnBounds(true);
        searchIcon.setOnMouseClicked(e -> search());

        // ===== Search Box =====
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

        // ===== Search Bar =====
        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(15));
        searchBar.getChildren().addAll(searchBox, cbbType);

        HBox.setHgrow(searchBox, Priority.ALWAYS);
        // ============== END SEARCH BAR =============

        // ================= CONTENT =================
        BorderPane.setMargin(resultArea, new Insets(15));
        // =============== END CONTENT ===============

        this.setTop(searchBar);
        this.setStyle("-fx-background-color: white;");
        this.setCenter(resultArea);
    }

    private void search() {
        String keyword = txtSearch.getText();
        String type = cbbType.getValue();
        // tést thử chức năng
        resultArea.setText("Searching " + type + ": " + keyword);
    }
}