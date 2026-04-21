package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.pages.MapSearchPage;
import geoinfo.client.gui.pages.SearchEnginePage;
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
    private MButton btnSearchPage;
    private MButton btnMapPage;

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
        mainLayout.getStyleClass().add("app-shell");
        header.getStyleClass().add("app-header");
        leftMenu.getStyleClass().add("left-menu-card");
        content.getStyleClass().add("content-shell");

        MButton title = new MButton("Geographic Information System", "/images/logo/globe.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        title.getStyleClass().add("app-title");
        header.setPadding(new Insets(12, 20, 12, 20));
        header.getChildren().add(title);

        btnSearchPage = new MButton("Search Engine", "");
        btnMapPage = new MButton("Map Search", "");
        btnSearchPage.getStyleClass().add("side-nav-button");
        btnMapPage.getStyleClass().add("side-nav-button");

        leftMenu.setSpacing(12);
        leftMenu.setPadding(new Insets(24, 16, 24, 16));
        leftMenu.getChildren().addAll(btnSearchPage, btnMapPage);
        leftMenu.setPrefWidth(Consts.APP_DEFAULT_WIDTH - Consts.CONTENT_DEFAULT_WIDTH);
        BorderPane.setMargin(leftMenu, new Insets(20, 0, 20, 20));

        btnSearchPage.setOnAction(e -> showSearchPage());
        btnMapPage.setOnAction(e -> showMapPage());

        showSearchPage();
        content.setPadding(new Insets(20, 24, 20, 16));

        mainLayout.setTop(header);
        mainLayout.setCenter(content);
        mainLayout.setLeft(leftMenu);

        Scene scene = new Scene(mainLayout, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/utils/Configure.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/utils/ModernTheme.css").toExternalForm());
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

    private void showSearchPage() {
        content.setCenter(searchEnginePage);
        setActiveMenu(btnSearchPage);
    }

    private void showMapPage() {
        content.setCenter(mapSearchPage);
        setActiveMenu(btnMapPage);
    }

    private void setActiveMenu(MButton activeButton) {
        btnSearchPage.getStyleClass().remove("side-nav-button-active");
        btnMapPage.getStyleClass().remove("side-nav-button-active");
        if (!activeButton.getStyleClass().contains("side-nav-button-active")) {
            activeButton.getStyleClass().add("side-nav-button-active");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
