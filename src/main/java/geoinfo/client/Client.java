package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.pages.MapSearchPage;
import geoinfo.client.gui.pages.SearchEnginePage;
import geoinfo.client.gui.utils.Configure;
import geoinfo.client.gui.utils.Consts;
import geoinfo.client.network.ClientService;
import geoinfo.server.network.ServerEndpoint;
import geoinfo.server.network.ServerRegistryApi;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Client extends Application {
    private ClientService clientService;
    private BorderPane mainLayout;
    private HBox header;
    private VBox leftMenu;
    private BorderPane content;
    private SearchEnginePage searchEnginePage;
    private MapSearchPage mapSearchPage;

    @Override
    public void start(Stage stage) {
        ServerEndpoint endpoint = ServerRegistryApi.fetchServerEndpointOrDefault();
        clientService = new ClientService(endpoint.ip(), endpoint.port());
        searchEnginePage = new SearchEnginePage(clientService);
        mapSearchPage = new MapSearchPage(searchEnginePage);
        mainLayout = new BorderPane();
        header = new HBox();
        leftMenu = new VBox();
        content = new BorderPane();

        MButton title = new MButton("Geographic Information System", "/images/logo/globe.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        header.setBackground(Configure.PRIMARY_BACKGROUND);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.getChildren().add(title);

        MButton btnSearchPage = new MButton("Search Engine", "");
        MButton btnMapPage = new MButton("Map Search", "");

        leftMenu.setSpacing(10);
        leftMenu.setPadding(new Insets(50, 20, 50, 40));
        leftMenu.getChildren().addAll(btnSearchPage, btnMapPage);
        leftMenu.setPrefWidth(Consts.APP_DEFAULT_WIDTH - Consts.CONTENT_DEFAULT_WIDTH);
        leftMenu.setBackground(Configure.SECONDARY_BACKGROUND);

        btnSearchPage.setOnAction(e -> content.setCenter(searchEnginePage));
        btnMapPage.setOnAction(e -> content.setCenter(mapSearchPage));

        content.setCenter(searchEnginePage);
        content.setPadding(new Insets(50, 40, 50, 20));

        mainLayout.setTop(header);
        mainLayout.setCenter(content);
        mainLayout.setLeft(leftMenu);
        mainLayout.setBackground(Configure.SECONDARY_BACKGROUND);

        Scene scene = new Scene(mainLayout, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/utils/Configure.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Geo Info");
        stage.show();

        if (!clientService.connect()) {
            searchEnginePage.setResult("Unable to connect to server at " + endpoint.asAddress() + "!\n");
        } else {
            searchEnginePage.setResult("Server connected: " + endpoint.asAddress() + "\n");
        }
    }

    @Override
    public void stop() {
        if (clientService != null) {
            clientService.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
