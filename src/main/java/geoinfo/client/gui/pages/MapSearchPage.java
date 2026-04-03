package geoinfo.client.gui.pages;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class MapSearchPage extends BorderPane {
    private Label mapTooltip;
    private Group map;

    public MapSearchPage(){
        initComponents();
        buildLayout();
    }

    public Group loadSvgMap(String filePath) throws Exception{
        Group mapGroup = new Group();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filePath));

        NodeList nodeList = doc.getElementsByTagName("path");
        for (int i=0; i<nodeList.getLength(); i++){
            Element element = (Element) nodeList.item(i);
            String pathData = element.getAttribute("d");
//            String countryID = element.getAttribute("id");
            String nameCountry = element.getAttribute("title");
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(pathData);
            svgPath.setFill(Color.AZURE);
            svgPath.setStroke(Color.BLACK);
            svgPath.setStrokeWidth(0.2);

            svgPath.setOnMouseEntered(e->{
                svgPath.setFill(Color.GRAY);
//                mapTooltip.setText(countryID);
                mapTooltip.setText(nameCountry);
                mapTooltip.setVisible(true);
            });
            svgPath.setOnMouseMoved(e -> {
                // Lấy tọa độ chuột so với cái Pane gốc (root)
                double mouseX = e.getSceneX();
                double mouseY = e.getSceneY();

                // Đặt vị trí Tooltip hơi lệch so với con trỏ chuột một chút để không che mất
                mapTooltip.setLayoutX(mouseX + 15);
                mapTooltip.setLayoutY(mouseY + 15);
            });

            // 3. Khi chuột đi RA KHỎI quốc gia
            svgPath.setOnMouseExited(e -> {
                svgPath.setFill(Color.AZURE); // Trả lại màu nền cũ
                mapTooltip.setVisible(false); // Ẩn Tooltip
            });

            mapGroup.getChildren().add(svgPath);
        }
        return mapGroup;
    }

//    private void clickMouse(){
//
//    }
    public void enableZoomAndPan(Pane container, Group content) {
        // Zoom bằng con lăn chuột
        final double[] mouseAnchor = new double[2];
        final double[] translateAnchor = new double[2];

        container.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY() > 0) ? 1.1 : 0.9;
            double newScale = content.getScaleX() * zoomFactor;

            // Giới hạn zoom từ 0.5 lần đến 10 lần
            if (newScale >= 0.5 && newScale <= 10.0) {
                content.setScaleX(newScale);
                content.setScaleY(newScale);
            }
            event.consume();
        });

        // 2. Xử lý KÉO (Pan) - Nhấn chuột xuống
        container.setOnMousePressed(event -> {
            // Lưu vị trí chuột lúc bắt đầu nhấn
            mouseAnchor[0] = event.getSceneX();
            mouseAnchor[1] = event.getSceneY();
            // Lưu vị trí hiện tại của bản đồ
            translateAnchor[0] = content.getTranslateX();
            translateAnchor[1] = content.getTranslateY();
        });

        // 3. Xử lý KÉO (Pan) - Di chuyển chuột
        container.setOnMouseDragged(event -> {
            // Tính toán khoảng cách đã di chuyển và cập nhật vị trí mới
            content.setTranslateX(translateAnchor[0] + event.getSceneX() - mouseAnchor[0]);
            content.setTranslateY(translateAnchor[1] + event.getSceneY() - mouseAnchor[1]);
        });

        container.setOnMouseDragged(event -> {
            // 1. Tính toán vị trí mới dự kiến dựa trên độ lệch chuột
            double deltaX = event.getSceneX() - mouseAnchor[0];
            double deltaY = event.getSceneY() - mouseAnchor[1];

            double newTranslateX = translateAnchor[0] + deltaX;
            double newTranslateY = translateAnchor[1] + deltaY;

            // 2. Lấy kích thước thực tế của bản đồ SAU KHI ZOOM (Bounds In Parent)
            // Bounds này đã bao gồm cả ScaleX/ScaleY
            var bounds = content.getBoundsInParent();
            double mapWidth = bounds.getWidth();
            double mapHeight = bounds.getHeight();

            // 3. Lấy kích thước của cái khung chứa (Vùng trống bên phải Sidebar)
            double viewWidth = container.getWidth();
            double viewHeight = container.getHeight();

            // 4. Logic giới hạn thông minh:
            // Nếu bản đồ to hơn khung hình, cho phép kéo nhưng không được lòi khoảng trắng
            // Nếu bản đồ nhỏ hơn khung hình, có thể ép nó nằm giữa

            // Giới hạn không cho tâm bản đồ bay quá xa khỏi khung
            double limitX = mapWidth / 2;
            double limitY = mapHeight / 2;

            if (Math.abs(newTranslateX) > limitX) newTranslateX = Math.signum(newTranslateX) * limitX;
            if (Math.abs(newTranslateY) > limitY) newTranslateY = Math.signum(newTranslateY) * limitY;

            // 5. Cập nhật
            content.setTranslateX(newTranslateX);
            content.setTranslateY(newTranslateY);
        });
    }

    private void initComponents() {
        try {
            String path = System.getProperty("user.dir") + "/src/main/resources/data/world.svg";
            map = loadSvgMap(path);

            // 3. Khởi tạo Tooltip
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
            System.err.println("Lỗi load map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildLayout() {
        Pane mapContainer = new Pane();
        mapContainer.getChildren().add(map);
        mapContainer.getChildren().add(mapTooltip);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mapContainer.widthProperty());
        clip.heightProperty().bind(mapContainer.heightProperty());
        mapContainer.setClip(clip);
        enableZoomAndPan(mapContainer, map);

        this.setCenter(mapContainer);
    }
}
