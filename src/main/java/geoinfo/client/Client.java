package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.pages.MapSearchPage;
import geoinfo.client.gui.pages.SearchEnginePage;
import geoinfo.client.gui.utils.Configure;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Client extends Application {
    private ClientService clientService;

    private BorderPane mainLayout;
    private HBox header;
    private HBox toolBar;
    private VBox leftMenu;
    private BorderPane content;
    private SearchEnginePage searchEnginePage;
    private MapSearchPage mapSearchPage;

    @Override
    public void start(Stage stage) {
        clientService = new ClientService("127.0.0.1", 1234, 16384);
        mainLayout = new BorderPane();
        header = new HBox();
        toolBar = new HBox();
        leftMenu = new VBox();
        content = new BorderPane();
        searchEnginePage = new SearchEnginePage();
        mapSearchPage = new MapSearchPage();

        MButton title = new MButton("Geographic Information System", "/images/logo/globe.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        header.setBackground(Configure.PRIMARY_BACKGROUND);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.getChildren().add(title);

        toolBar.setBackground(Configure.SECONDARY_BACKGROUND);
        content.setTop(toolBar);

        MButton btnSearchPage = new MButton("Search Engine", "");
        MButton btnMapPage = new MButton("Map Search", "");

        leftMenu.setSpacing(10);
        leftMenu.setPadding(new Insets(15));
        leftMenu.getChildren().addAll(btnSearchPage, btnMapPage);
        leftMenu.setPrefWidth(Consts.APP_DEFAULT_WIDTH - Consts.CONTENT_DEFAULT_WIDTH);
        leftMenu.setBackground(Configure.SECONDARY_BACKGROUND);

        btnSearchPage.setOnAction(e -> content.setCenter(searchEnginePage));
        btnMapPage.setOnAction(e -> content.setCenter(mapSearchPage));

        content.setCenter(searchEnginePage);

        mainLayout.setTop(header);
        mainLayout.setCenter(content);
        mainLayout.setLeft(leftMenu);
        Scene scene = new Scene(mainLayout, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/utils/Configure.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Geo Info");
        stage.show();

        Text text = new Text("Chưa kết nối");
        mainLayout.getChildren().add(text);
        Button btn = new Button("Connect");
        mainLayout.getChildren().add(btn);

        btn.setOnAction(e -> {
            text.setText("Đang kết nối...");
            new Thread(clientService::start).start();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
