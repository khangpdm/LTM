package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.SearchResultPane;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class MapSearchPage extends BorderPane {
    private final SearchEnginePage searchEnginePage;
    private Label mapTooltip;
    private Group map;
    private Pane mapContainer;

    public MapSearchPage(SearchEnginePage searchEnginePage) {
        this.searchEnginePage = searchEnginePage;
        initComponents();
        buildLayout();
    }

    public Group loadSvgMap(String filePath) throws Exception {
        Group mapGroup = new Group();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filePath));

        NodeList nodeList = doc.getElementsByTagName("path");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String pathData = element.getAttribute("d");
            String nameCountry = element.getAttribute("title");
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(pathData);
            svgPath.setFill(Color.AZURE);
            svgPath.setStroke(Color.BLACK);
            svgPath.setStrokeWidth(0.2);

            svgPath.setOnMouseEntered(event -> {
                svgPath.setFill(Color.GRAY);
                mapTooltip.setText(nameCountry);
                mapTooltip.setVisible(true);
            });
            svgPath.setOnMouseMoved(event -> {
                Point2D localMouse = mapContainer.sceneToLocal(event.getSceneX(), event.getSceneY());
                mapTooltip.setLayoutX(localMouse.getX() + 15);
                mapTooltip.setLayoutY(localMouse.getY() + 15);
            });
            svgPath.setOnMouseExited(event -> {
                svgPath.setFill(Color.AZURE);
                mapTooltip.setVisible(false);
            });
            svgPath.setOnMouseClicked(event -> {
                event.consume();
                showCountryPopup(nameCountry);
            });

            mapGroup.getChildren().add(svgPath);
        }
        return mapGroup;
    }

    public void enableZoomAndPan(Pane container, Group content) {
        final double[] mouseAnchor = new double[2];
        final double[] translateAnchor = new double[2];

        container.setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            double newScale = content.getScaleX() * zoomFactor;

            if (newScale >= 0.5 && newScale <= 10.0) {
                content.setScaleX(newScale);
                content.setScaleY(newScale);
            }
            event.consume();
        });

        container.setOnMousePressed(event -> {
            mouseAnchor[0] = event.getSceneX();
            mouseAnchor[1] = event.getSceneY();
            translateAnchor[0] = content.getTranslateX();
            translateAnchor[1] = content.getTranslateY();
        });

        container.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseAnchor[0];
            double deltaY = event.getSceneY() - mouseAnchor[1];

            double newTranslateX = translateAnchor[0] + deltaX;
            double newTranslateY = translateAnchor[1] + deltaY;

            var bounds = content.getBoundsInParent();
            double mapWidth = bounds.getWidth();
            double mapHeight = bounds.getHeight();

            double limitX = mapWidth / 2;
            double limitY = mapHeight / 2;

            if (Math.abs(newTranslateX) > limitX) {
                newTranslateX = Math.signum(newTranslateX) * limitX;
            }
            if (Math.abs(newTranslateY) > limitY) {
                newTranslateY = Math.signum(newTranslateY) * limitY;
            }

            content.setTranslateX(newTranslateX);
            content.setTranslateY(newTranslateY);
        });
    }

    private void initComponents() {
        try {
            String path = System.getProperty("user.dir") + "/src/main/resources/data/world.svg";
            map = loadSvgMap(path);

            mapTooltip = new Label("");
            mapTooltip.setStyle(
                    "-fx-background-color: rgba(30, 30, 30, 0.85);" +
                            "-fx-text-fill: white;" +
                            "-fx-padding: 5px 10px;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-font-weight: bold;"
            );
            mapTooltip.setMouseTransparent(true);
            mapTooltip.setVisible(false);

        } catch (Exception e) {
            System.err.println("Loi load map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildLayout() {
        mapContainer = new Pane();
        mapContainer.getChildren().add(map);
        mapContainer.getChildren().add(mapTooltip);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mapContainer.widthProperty());
        clip.heightProperty().bind(mapContainer.heightProperty());
        mapContainer.setClip(clip);
        enableZoomAndPan(mapContainer, map);

        setCenter(mapContainer);
    }

    private void showCountryPopup(String countryName) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            popup.initOwner(getScene().getWindow());
        }
        popup.setTitle("Country Search: " + countryName);

        Label title = new Label(countryName);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

//        SearchResultPane resultPane = searchEnginePage.createResultPane();
//        resultPane.setText("Dang tim kiem quoc gia...");

        BorderPane root = new BorderPane();
        root.setTop(title);
//        root.setCenter(resultPane);
        root.setPadding(new Insets(16));
        BorderPane.setMargin(title, new Insets(0, 0, 12, 0));

        popup.setScene(new Scene(root, 760, 560));
        popup.show();

//        resultPane.search("country:" + countryName, "Dang tim kiem quoc gia...");
    }
}
