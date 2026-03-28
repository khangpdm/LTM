package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.utils.*;
import geoinfo.client.gui.pages.*;
import geoinfo.client.network.ClientService;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Client extends Application {
    private ClientService clientService;

    private BorderPane mainLayout;
    private HBox header, toolBar;
    private VBox leftMenu;
    private BorderPane content;
    private SearchEnginePage searchEnginePage;
    private MapSearchPage mapSearchPage;

    @Override
    public void start(Stage stage) {
        clientService = new ClientService("localhost", 12345);
        mainLayout = new BorderPane();
        header = new HBox();
        leftMenu = new VBox();
        content = new BorderPane();
        searchEnginePage = new SearchEnginePage();
        mapSearchPage = new MapSearchPage();

        // ================== HEADER =================
        MButton title = new MButton("Geographic Information System", "/images/logo/globe_1.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        header.setBackground(Configure.PRIMARY_BACKGROUND);
        header.setPadding(new Insets(10,20,10,20));
        header.getChildren().add(title);
        // ================ END HEADER ===============


        // ================ LEFT MENU ================
        MButton btnSearchPage = new MButton("Search Engine", "");
        MButton btnMapPage = new MButton("Map Search", "");

        leftMenu.setSpacing(10);
        leftMenu.setPadding(new Insets(15));
        leftMenu.getChildren().addAll(btnSearchPage, btnMapPage);
        leftMenu.setPrefWidth(Consts.APP_DEFAULT_WIDTH - Consts.CONTENT_DEFAULT_WIDTH);
        leftMenu.setBackground(Configure.SECONDARY_BACKGROUND);

        // CHUYỂN TRANG
        btnSearchPage.setOnAction(e -> content.setCenter(searchEnginePage));
        btnMapPage.setOnAction(e -> content.setCenter(mapSearchPage));
        // ============== END LEFT MENU ==============


        // ================= CONTENT =================
        content.setCenter(searchEnginePage);
        // =============== END CONTENT ===============


        // =============== MAIN LAYOUT ===============
        mainLayout.setTop(header);
        mainLayout.setCenter(content);
        mainLayout.setLeft(leftMenu);
        Scene scene = new Scene(mainLayout, Consts.APP_DEFAULT_WIDTH, Consts.APP_DEFAULT_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/utils/Configure.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Geo Ìnfo");
        stage.show();
        // ============= END MAIN LAYOUT =============


        //Button btn = new Button("Connect");
        //mainLayout.getChildren().add(btn);
        //content.setTop(btn);
        //btn.setOnAction(e -> {
        //    new Thread(() -> {clientService.start();}).start();
        //});
    }

    public static void main(String[] args) {
        launch(args);
    }
}