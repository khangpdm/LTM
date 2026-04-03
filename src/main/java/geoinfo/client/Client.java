package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.utils.*;
import geoinfo.client.gui.pages.*;
import geoinfo.client.network.ClientService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
<<<<<<< HEAD
import javafx.scene.control.*;
=======
>>>>>>> cf52dc9c88c9297194db9eacfab0d095be8028df
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Client extends Application {
    private ClientService clientService;
    private BorderPane mainLayout;
<<<<<<< HEAD
    private HBox header, toolBar;
=======
    private HBox header;
    private VBox leftMenu;
>>>>>>> cf52dc9c88c9297194db9eacfab0d095be8028df
    private BorderPane content;
    private SearchEnginePage searchEnginePage;
    private MapSearchPage mapSearchPage;

    @Override
    public void start(Stage stage) {
        clientService = new ClientService("localhost", 12345);
        searchEnginePage = new SearchEnginePage(clientService);
        mapSearchPage = new MapSearchPage(); // them clientService vao sau
        mainLayout = new BorderPane();
        header = new HBox();
<<<<<<< HEAD
        toolBar = new HBox();
=======
        leftMenu = new VBox();
>>>>>>> cf52dc9c88c9297194db9eacfab0d095be8028df
        content = new BorderPane();

        // ================== HEADER =================
        MButton title = new MButton("Geographic Information System", "/images/logo/globe.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        header.setBackground(Configure.PRIMARY_BACKGROUND);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.getChildren().add(title);
        // ================ END HEADER ===============

<<<<<<< HEAD
        // ================= TOOLBAR =================
        toolBar.setBackground(Configure.SECONDARY_BACKGROUND);
        toolBar.setPadding(new Insets(10, 20, 10, 20));
        toolBar.setSpacing(10);

        // Label + ComboBox để chọn loại tìm kiếm
        Label typeLabel = new Label("Loại tìm kiếm:");
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        ComboBox<String> queryType = new ComboBox<>();
        queryType.getItems().addAll("Country", "City");
        queryType.setValue("Country");
        queryType.setPrefWidth(120);
        queryType.setStyle("-fx-font-size: 13;");

        // Label + TextField để nhập tên quốc gia/thành phố
        Label searchLabel = new Label("Tìm kiếm:");
        searchLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        TextField searchInput = new TextField();
        searchInput.setPromptText("Nhập tên quốc gia hoặc thành phố...");
        searchInput.setPrefWidth(350);
        searchInput.setStyle("-fx-font-size: 13;");

        // Button tìm kiếm
        Button searchBtn = new Button("🔍 Tìm kiếm");
        searchBtn.setStyle("-fx-font-size: 13; -fx-padding: 8px 20px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        searchBtn.setCursor(javafx.scene.Cursor.HAND);

        // Status label
        Label statusLabel = new Label("Sẵn sàng");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");

        toolBar.getChildren().addAll(
            typeLabel, queryType,
            new Separator(Orientation.VERTICAL),
            searchLabel, searchInput, searchBtn,
            new Separator(Orientation.VERTICAL),
            statusLabel
        );
        // =============== END TOOLBAR ===============

        // ================= CONTENT =================
        TextArea resultArea = new TextArea();
        resultArea.setWrapText(true);
        resultArea.setEditable(false);
        resultArea.setStyle("-fx-control-inner-background: #f0f0f0; -fx-text-fill: #333; -fx-font-size: 12; -fx-font-family: 'Courier New';");
        resultArea.setText("Chào mừng bạn đến với Geographic Information System!\n\n" +
                          "Hướng dẫn sử dụng:\n" +
                          "1. Chọn loại tìm kiếm: Country (Quốc gia) hoặc City (Thành phố)\n" +
                          "2. Nhập tên quốc gia/thành phố\n" +
                          "3. Nhấn 'Tìm kiếm' hoặc Enter\n\n" +
                          "Đợi kết quả sẽ hiển thị ở đây...");

        content.setTop(toolBar);
        content.setCenter(resultArea);
=======
        // ================ LEFT MENU ================
        MButton btnSearchPage = new MButton("Search Engine", "");
        MButton btnMapPage = new MButton("Map Search", "");

        leftMenu.setSpacing(10);
        leftMenu.setPadding(new Insets(50, 20, 50 ,40));
        leftMenu.getChildren().addAll(btnSearchPage, btnMapPage);
        leftMenu.setPrefWidth(Consts.APP_DEFAULT_WIDTH - Consts.CONTENT_DEFAULT_WIDTH);
        leftMenu.setBackground(Configure.SECONDARY_BACKGROUND);

        btnSearchPage.setOnAction(e -> content.setCenter(searchEnginePage));
        btnMapPage.setOnAction(e -> content.setCenter(mapSearchPage));
        // ============== END LEFT MENU ==============

        // ================= CONTENT =================
        content.setCenter(searchEnginePage);
        content.setPadding(new Insets(50, 40, 50 ,20));
>>>>>>> cf52dc9c88c9297194db9eacfab0d095be8028df
        // =============== END CONTENT ===============

        // =============== MAIN LAYOUT ===============
        mainLayout.setTop(header);
        mainLayout.setCenter(content);
<<<<<<< HEAD

        Scene scene = new Scene(mainLayout, 1400, 750);
        try {
            String css = getClass().getResource("/utils/Configure.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (NullPointerException e) {
            System.out.println("CSS file not found, using default styling");
        }

        stage.setScene(scene);
        stage.setTitle("Geographic Information System");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
        // ============= END MAIN LAYOUT =============

        // =============== EVENT HANDLERS ===============
        // Search button handler
        searchBtn.setOnAction(e -> performSearch(searchInput, queryType, resultArea, statusLabel));

        // Enter key handler
        searchInput.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                performSearch(searchInput, queryType, resultArea, statusLabel);
            }
        });

    }

    private void performSearch(TextField searchInput, ComboBox<String> queryType, TextArea resultArea, Label statusLabel) {
        String input = searchInput.getText().trim();
        String type = queryType.getValue();

        if (input.isEmpty()) {
            resultArea.setText("⚠️ Vui lòng nhập tên để tìm kiếm!");
            return;
        }

        statusLabel.setText("⏳ Đang tìm kiếm...");
        resultArea.setText("Đang xử lý yêu cầu...\n");

        new Thread(() -> {
            try {
                String query = type.equalsIgnoreCase("Country")
                    ? "country:" + input
                    : "city:" + input;

                ClientService service = new ClientService("localhost", 12345);
                service.sendQuery(query, response -> {
                    javafx.application.Platform.runLater(() -> {
                        resultArea.setText(response);
                        statusLabel.setText("✓ Hoàn thành");
                    });
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    resultArea.setText("❌ Lỗi: " + ex.getMessage());
                    statusLabel.setText("✗ Lỗi kết nối");
                });
            }
        }).start();
=======
        mainLayout.setLeft(leftMenu);
        mainLayout.setBackground(Configure.SECONDARY_BACKGROUND);
        Scene scene = new Scene(mainLayout, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/utils/Configure.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Geo Info");
        stage.show();
        // ============= END MAIN LAYOUT =============

        if (!clientService.connect()) {
            searchEnginePage.setResult("Unable to connect to server!\n");
        } else {
            searchEnginePage.setResult("Server connected!\n");
        }
    }

    @Override
    public void stop() {
        if (clientService != null) {
            clientService.disconnect();
        }
>>>>>>> cf52dc9c88c9297194db9eacfab0d095be8028df
    }

    public static void main(String[] args) {
        launch(args);
    }
}