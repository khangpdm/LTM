package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.SearchResultPane;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import geoinfo.client.utils.Validation;
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

    // 1. CONSTRUCTOR KHỞI TẠO ĐỐI TƯỢNG CHÍNH
    public SearchEnginePage(ClientService clientService) {
        this.clientService = clientService;
        this.resultPane = new SearchResultPane(clientService);
        initComponents();
        buildLayout();
    }

    // 1.1. Khởi tạo tất cả các thành phần GUI
    private void initComponents() {
        txtSearch = new TextField();
        txtSearch.setPromptText("Enter the keyword to search ...");
        txtSearch.setPrefHeight(Consts.SEARCHBAR_ITEM_HEIGHT);
        txtSearch.getStyleClass().add("txt-search-style");
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
                if (!getStyleClass().contains("cbb-display-cell")) {
                    getStyleClass().add("cbb-display-cell");
                }
            }
        });
        cbbType.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!getStyleClass().contains("cbb-popup-cell")) {
                    getStyleClass().add("cbb-popup-cell");
                }
            }
        });

        pnlContent = new BorderPane();
        pnlContent.getStyleClass().add("search-content-panel");
    }

    // 1.2. Sắp xếp bố cục các thành phần
    private void buildLayout() {
        searchIcon.setPickOnBounds(true);
        searchIcon.setOnMouseClicked(event -> search());

        HBox searchBox = new HBox(0);
        searchBox.getStyleClass().add("search-box-style");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        StackPane iconWrapper = new StackPane(searchIcon);
        iconWrapper.setMinWidth(Consts.SEARCHBAR_ITEM_HEIGHT);
        iconWrapper.setPrefWidth(Consts.SEARCHBAR_ITEM_HEIGHT);
        searchBox.getChildren().addAll(iconWrapper, txtSearch);

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(0, 15, 0, 15));
        searchBar.getChildren().addAll(searchBox, cbbType);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        BorderPane.setMargin(pnlContent, new Insets(12, 15, 12, 15));
        Label lblContent = new Label("Search Results");
        lblContent.getStyleClass().add("search-content-title");
        pnlContent.setTop(lblContent);
        pnlContent.setCenter(resultPane);

        this.setTop(searchBar);
        this.getStyleClass().add("search-page");
        this.setCenter(pnlContent);
    }

    // 2. XỬ LÝ SỰ KIỆN TÌM KIẾM KHI NGƯỜI DÙNG NHẤN ENTER HOẶC CLICK ICON TÌM KIẾM
    private void search() {
        String originalKeyword = txtSearch.getText();
        String keyword = Validation.sanitizeInput(originalKeyword);
        String type = cbbType.getValue();

        if (Validation.isEmpty(keyword)) {
            resultPane.setText("No results found. Try a different search term.");
            return;
        }
        if (keyword.length() > 100) {
            resultPane.setText("Keyword is too long.");
            return;
        }
        if (!Validation.isValidLocationName(keyword)) {
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

    // 3. HIỂN THỊ KẾT QUẢ TÌM KIẾM DẠNG TEXT LÊN RESULTPANE (DÙNG CHO CÁC TRƯỜNG HỢP ĐẶC BIỆT)
    public void setResult(String result) {
        resultPane.setText(result);
    }

    // 4. XÓA TOÀN BỘ KẾT QUẢ HIỂN THỊ TRÊN RESULTPANE
    public void clearResult() {resultPane.clear();}

    // 5. TẠO VÀ TRẢ VỀ MỘT INSTANCE MỚI CỦA SEARCHRESULTPANE
    public SearchResultPane createResultPane() {
        return new SearchResultPane(clientService);
    }
}
