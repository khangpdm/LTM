package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.utils.Configure;
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


    @Override
    public void start(Stage stage) {
        clientService = new ClientService("localhost", 12345);
        mainLayout = new BorderPane();
        header = new HBox();
        toolBar = new HBox();
        leftMenu = new VBox();
        content = new BorderPane();

        // ================== HEADER =================
        MButton title = new MButton("Geographic Information System", "/images/logo/globe.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        header.setBackground(Configure.PRIMARY_BACKGROUND);
        header.setPadding(new Insets(10,20,10,20));
        header.getChildren().add(title);
        // ================ END HEADER ===============


        // ================= CONTENT =================
        toolBar.setBackground(Configure.SECONDARY_BACKGROUND);
        content.setTop(toolBar);
        // =============== END CONTENT ===============


        // ================ LEFT MENU ================


        // ============== END LEFT MENU ==============

        // =============== MAIN LAYOUT ===============
        mainLayout.setTop(header);
        mainLayout.setCenter(content);
        mainLayout.setLeft(leftMenu);
        Scene scene = new Scene(mainLayout, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/utils/Configure.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Geo Ìnfo");
        stage.show();
        // ============= END MAIN LAYOUT =============



        Text text = new Text("Chưa kết nối");
        mainLayout.getChildren().add(text);
        Button btn = new Button("Connect");
        mainLayout.getChildren().add(btn);

        btn.setOnAction(e -> {
            text.setText("Đang kết nối...");
            new Thread(() -> {clientService.start();}).start();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}