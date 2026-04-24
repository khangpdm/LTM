package geoinfo.client.gui.components;

import geoinfo.client.network.ClientService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class SearchResultPane extends VBox {
    private static final ExecutorService SEARCH_EXECUTOR = Executors.newCachedThreadPool(new SearchThreadFactory());

    private final AtomicLong latestRequestId = new AtomicLong();
    private final ClientService clientService;

    private ImageView flagPreview;
    private Label titleLabel;
    private Button moreInfoButton;
    private VBox resultContainer;
    private ScrollPane resultScrollPane;

    private String pendingMoreInfoRequest;
    private String loadedMoreInfoRequest;

    private JSONObject cachedMoreInfo;
    private boolean isLoadingMoreInfo;
    private JSONObject cachedHotels;
    private boolean isLoadingHotels;

    // 1. CONSTRUCTOR KHỞI TẠO ĐỐI TƯỢNG CHÍNH
    public SearchResultPane(ClientService clientService) {
        this.clientService = clientService;
        initComponent();
        buildLayout();
    }

    // 1.1. Khởi tạo tất cả các thành phần GUI
    private void initComponent(){
        flagPreview = new ImageView();
        flagPreview.setFitHeight(36);
        flagPreview.setFitWidth(54);
        flagPreview.setPreserveRatio(true);
        flagPreview.setVisible(false);
        flagPreview.setManaged(false);

        titleLabel = new Label("");
        titleLabel.getStyleClass().add("title-label-style");
        titleLabel.setVisible(false);
        titleLabel.setManaged(false);

        moreInfoButton = new Button("See More Information");
        moreInfoButton.getStyleClass().add("more-info-button");
        moreInfoButton.setVisible(false);
        moreInfoButton.setManaged(false);
        moreInfoButton.setDisable(true);
        moreInfoButton.setOnAction(event -> fetchMoreInfo());

        resultContainer = new VBox(12);
        resultContainer.setPadding(new Insets(12));
        resultContainer.getStyleClass().add("result-container");

        resultScrollPane = new ScrollPane(resultContainer);
        resultScrollPane.setFitToWidth(true);
        resultScrollPane.setFitToHeight(true);
        resultScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultScrollPane.getStyleClass().add("result-scroll");
    }

    // 1.2. Sắp xếp bố cục các thành phần
    private void buildLayout() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerBar = new HBox(12, flagPreview, titleLabel, spacer, moreInfoButton);
        headerBar.getStyleClass().add("result-header");
        headerBar.setAlignment(Pos.CENTER_LEFT);

        this.setSpacing(12);
        this.getChildren().addAll(headerBar, resultScrollPane);
        VBox.setVgrow(resultScrollPane, Priority.ALWAYS);

        this.setFillWidth(true);
    }

    // 2. HIỂN THỊ MỘT THÔNG BÁO VĂN BẢN
    public void setText(String text) {
        clearSupplementaryUi();
        showMessage(text);
    }

    // 3. XÓA TOÀN BỘ NỘI DUNG TRONG RESULTCONTAINER VÀ CÁC THÀNH PHẦN PHỤ TRỢ
    public void clear() {
        clearSupplementaryUi();
        resultContainer.getChildren().clear();
    }

    // 4. GỬI REQUEST TÌM KIẾM LÊN SERVER BẤT ĐỒNG BỘ, HIỂN THỊ MESSAGE LOADING, SAU ĐÓ RENDER KẾT QUẢ TRẢ VỀ
    public void search(String request, String loadingMessage) {
        long requestId = latestRequestId.incrementAndGet();
        pendingMoreInfoRequest = null;
        loadedMoreInfoRequest = null;
        clearSupplementaryUi();
        showMessage(loadingMessage);

        SEARCH_EXECUTOR.submit(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "!!! ERROR WHEN SEARCHING: " + ex.getMessage();
            }

            String finalResponse = response;
            Platform.runLater(() -> {
                if (requestId != latestRequestId.get()) {
                    return;
                }
                renderResponse(finalResponse, false);
            });
        });
    }

    // 5. TẢI THÊM THÔNG TIN CHI TIẾT (MORE INFO) TỪ SERVER KHI NGƯỜI DÙNG NHẤN NÚT (CÓ HỖ TRỢ CACHE)
    private void fetchMoreInfo() {
        String request = pendingMoreInfoRequest;
        if (request == null || request.isBlank()) { return; }
        if (request.equals(loadedMoreInfoRequest)) { return; }
        // Note: Sử lý load dữ liệu dùng cache nếu đã có
        if (cachedMoreInfo != null) {
            renderResponse(cachedMoreInfo.toString(), true);
            loadedMoreInfoRequest = request;
            moreInfoButton.setText("Information has been added");
            moreInfoButton.setDisable(true);
            return;
        }
        // Note: Nếu đang loading thì chờ
        if (isLoadingMoreInfo) {
            moreInfoButton.setDisable(true);
            moreInfoButton.setText("Information is being loaded...");
            // Note: Poll chờ cache sẵn sàng
            SEARCH_EXECUTOR.submit(() -> {
                while (isLoadingMoreInfo) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                Platform.runLater(this::fetchMoreInfo);
            });
            return;
        }
        // Note: (Fallback) gọi API như cũ nếu không có cache
        long requestId = latestRequestId.get();
        moreInfoButton.setDisable(true);
        moreInfoButton.setText("Dang tai them...");

        SEARCH_EXECUTOR.submit(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "Loi khi tai them thong tin: " + ex.getMessage();
            }

            String finalResponse = response;
            Platform.runLater(() -> {
                if (requestId != latestRequestId.get()) {
                    return;
                }
                renderResponse(finalResponse, true);
                loadedMoreInfoRequest = request;
                moreInfoButton.setText("Da them thong tin");
                moreInfoButton.setDisable(true);
            });
        });
    }

    // 6. PHÂN TÍCH JSON RESPONSE TỪ SERVER VÀ HIỂN THỊ LÊN GIAO DIỆN Ở CHẾ ĐỘ GHI ĐÈ
    private void renderResponse(String response, boolean appendMode) {
        try {
            JSONObject json = new JSONObject(response);
            if (!"success".equalsIgnoreCase(json.optString("status"))) {
                if (!appendMode) {
                    clearSupplementaryUi();
                    showMessage(json.optString("message", response));
                } else {
                    appendMessage(json.optString("message", response));
                }
                return;
            }

            if (!appendMode) {
                applyHeader(json);
                showJsonAsTables(json);
            } else {
                appendAdditionalSections(json);
            }
        } catch (Exception e) {
            if (!appendMode) {
                clearSupplementaryUi();
                showMessage(response);
            } else {
                appendMessage(response);
            }
        }
    }
    // 6.1. Cập nhật phần header gồm flag, tiêu đề và nút "See More Information"
    private void applyHeader(JSONObject json) {
        String flagUrl = json.optString("flagUrl", "").trim();
        if (flagUrl.isBlank()) {
            flagPreview.setImage(null);
            flagPreview.setVisible(false);
            flagPreview.setManaged(false);
        } else {
            flagPreview.setImage(new Image(flagUrl, true));
            flagPreview.setVisible(true);
            flagPreview.setManaged(true);
        }

        String title = buildTitle(json);
        titleLabel.setText(title);
        titleLabel.setVisible(!title.isBlank());
        titleLabel.setManaged(!title.isBlank());

        pendingMoreInfoRequest = json.optString("moreInfoRequest", "").trim();
        loadedMoreInfoRequest = null;
        cachedMoreInfo = null;           // === THÊM: reset cache ===
        isLoadingMoreInfo = false;       // === THÊM: reset loading flag ===

        if (pendingMoreInfoRequest.isBlank()) {
            moreInfoButton.setVisible(false);
            moreInfoButton.setManaged(false);
            moreInfoButton.setDisable(true);
            moreInfoButton.setText("Them thong tin");
        } else {
            moreInfoButton.setVisible(true);
            moreInfoButton.setManaged(true);
            moreInfoButton.setDisable(false);
            moreInfoButton.setText(json.optString("moreInfoLabel", "Them thong tin"));
            // Note:: Prefetch more info trong background
            prefetchMoreInfo(pendingMoreInfoRequest);

            String hotelsRequest = json.optString("hotelsRequest", "").trim();
            if (!hotelsRequest.isBlank()) {
                prefetchHotels(hotelsRequest);
            }
        }
    }
    // 6.1.1. Tải trước dữ liệu "more info" trong background và lưu vào cachedMoreInfo
    private void prefetchMoreInfo(String request) {
        if (request == null || request.isBlank()) {return;}

        long requestId = latestRequestId.get();
        isLoadingMoreInfo = true;
        cachedMoreInfo = null;

        SEARCH_EXECUTOR.submit(() -> {
            JSONObject result = null;
            try {
                String response = clientService.sendRequest(request);
                JSONObject json = new JSONObject(response);
                if ("success".equalsIgnoreCase(json.optString("status"))) {
                    result = json;
                }
            } catch (Exception ex) {
                // Ignore error
            }

            JSONObject finalResult = result;
            Platform.runLater(() -> {
                if (requestId == latestRequestId.get()) {
                    cachedMoreInfo = finalResult;
                    isLoadingMoreInfo = false;
                }
            });
        });
    }
    //6.2.2. Tải trước dữ liệu khách sạn trong background, lưu vào cachedHotels và tự động append khi load xong
    private void prefetchHotels(String request) {
        if (request == null || request.isBlank()) {return;}

        long requestId = latestRequestId.get();
        isLoadingHotels = true;

        SEARCH_EXECUTOR.submit(() -> {
            JSONObject result = null;
            try {
                String response = clientService.sendRequest(request);
                JSONObject json = new JSONObject(response);
                if ("success".equalsIgnoreCase(json.optString("status"))) {
                    result = json;
                }
            } catch (Exception ex) {
                // Ignore error
            }

            JSONObject finalResult = result;
            Platform.runLater(() -> {
                if (requestId == latestRequestId.get()) {
                    cachedHotels = finalResult;
                    isLoadingHotels = false;

                    // Tự động append hotels khi load xong
                    if (cachedHotels != null) {
                        JSONArray hotels = cachedHotels.optJSONArray("hotels");
                        if (hotels != null && !hotels.isEmpty()) {
                            addHotelSection("Hotels", hotels);
                        }
                    }
                }
            });
        });
    }
    // 6.2. Hiển thị toàn bộ nội dung chính của JSON dưới dạng các bảng hoặc component đặc biệt.
    private void showJsonAsTables(JSONObject json) {
        resultContainer.getChildren().clear();

        Node infoNode = createTopInfoNode(json);
        if (infoNode != null) {
            resultContainer.getChildren().add(infoNode);
        }

        addNewsSection("News", json.optJSONArray("news"));
        addHotelSection("Hotels", json.optJSONArray("hotels"));
        addAttractionSection("Attractions", json.optJSONArray("attractions"));
    }
    // 6.2.1. Tạo component hiển thị thông tin tổng quan cho city hoặc country
    private Node createTopInfoNode(JSONObject json) {
        String type = json.optString("type", "").trim();
        if ("city".equalsIgnoreCase(type) || "country".equalsIgnoreCase(type)) {
            CityAndCountryComponent component = new CityAndCountryComponent();
            component.setItems(buildCardItems(json, type));
            return component;
        }
        return null;
    }
    // 6.2.2. Thêm section tin tức (News) vào resultContainer (nếu cần)
    private void addNewsSection(String title, JSONArray array) {
        if (array == null || array.isEmpty()) {
            return;
        }
        Label sectionLabel = createSectionLabel(title);
        resultContainer.getChildren().add(sectionLabel);
        VBox newsContainer = new VBox(20);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            newsContainer.getChildren().add(new NewsComponent(item));
        }
        if (!newsContainer.getChildren().isEmpty()) {
            resultContainer.getChildren().add(newsContainer);
        }
    }
    // 6.2.3. Thêm section điểm tham quan (Attraction) vào resultContainer (nếu cần)
    private void addAttractionSection(String title, JSONArray array) {
        if (array == null || array.isEmpty()) {
            return;
        }
        Label sectionLabel = createSectionLabel(title);
        resultContainer.getChildren().add(sectionLabel);
        VBox sectionBox = new VBox(20);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            sectionBox.getChildren().add(AttractionAndHotelComponent.forAttraction(item));
        }
        if (!sectionBox.getChildren().isEmpty()) {
            resultContainer.getChildren().add(sectionBox);
        }
    }
    // 6.2.4. Thêm section khách sạn (Hotel) vào resultContainer (nếu cần)
    private void addHotelSection(String title, JSONArray array) {
        if (array == null || array.isEmpty()) {
            return;
        }
        Label sectionLabel = createSectionLabel(title);
        resultContainer.getChildren().add(sectionLabel);
        VBox sectionBox = new VBox(20);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            sectionBox.getChildren().add(AttractionAndHotelComponent.forHotel(item));
        }
        if (!sectionBox.getChildren().isEmpty()) {
            resultContainer.getChildren().add(sectionBox);
        }
    }
    // 6.3. Nối thêm các section News, Hotels, Attractions vào resultContainer (dùng cho chế độ appendMode)
    private void appendAdditionalSections(JSONObject json) {
        addNewsSection("Some news", json.optJSONArray("news"));
        addHotelSection("Some hotels", json.optJSONArray("hotels"));
        addAttractionSection("Some attractions", json.optJSONArray("attractions"));

        String message = json.optString("message", "").trim();
        if (!message.isBlank()) {
            appendMessage(message);
        }
    }

    // 7. CÁC HÀM PHỤ TRỢ KHÁC
    private Node createImagePreview(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return null;
        }

        ImageView view = new ImageView(new Image(trimmed, true));
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitHeight(220);

        VBox wrapper = new VBox(view);
        wrapper.setFillWidth(true);
        wrapper.setPadding(new Insets(6, 0, 6, 0));
        view.fitWidthProperty().bind(wrapper.widthProperty());
        return wrapper;
    }

    private List<CityAndCountryComponent.Item> buildCardItems(JSONObject json, String type) {
        List<String> preferredOrder = new ArrayList<>();
        if ("country".equalsIgnoreCase(type)) {
            preferredOrder.add("neighboringCountries");
            preferredOrder.add("currentWeather");
            preferredOrder.add("languages");
            preferredOrder.add("coordinates");
            preferredOrder.add("population");
            preferredOrder.add("currencies");
        } else if ("city".equalsIgnoreCase(type)) {
            // Backward compatibility: older server responses may still return latitude/longitude instead of coordinates.
            if (!json.has("coordinates")) {
                double lat = json.optDouble("latitude", Double.NaN);
                double lon = json.optDouble("longitude", Double.NaN);
                if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                    json.put("coordinates", new JSONArray().put(lon).put(lat));
                }
            }

            preferredOrder.add("country");
            preferredOrder.add("population");
            preferredOrder.add("coordinates");
            preferredOrder.add("localTime");
            preferredOrder.add("temperatureCelsius");
            preferredOrder.add("weatherCondition");
            preferredOrder.add("humidity");
            preferredOrder.add("windKph");
        }

        List<String> keys = new ArrayList<>(json.keySet());
        keys.sort(String::compareToIgnoreCase);

        List<CityAndCountryComponent.Item> items = new ArrayList<>();

        for (String key : preferredOrder) {
            if (!json.has(key) || shouldSkipKey(key)) {
                continue;
            }
            items.add(new CityAndCountryComponent.Item(formatKey(key), stringify(json.opt(key))));
            keys.removeIf(k -> k.equals(key));
        }

        for (String key : keys) {
            if (shouldSkipKey(key)) {
                continue;
            }
            items.add(new CityAndCountryComponent.Item(formatKey(key), stringify(json.opt(key))));
        }

        return items;
    }

    private boolean shouldSkipKey(String key) {
        return "status".equals(key)
                || "type".equals(key)
                || "name".equals(key)
                || "region".equals(key)
                || "latitude".equals(key)
                || "longitude".equals(key)
                || "news".equals(key)
                || "hotels".equals(key)
                || "attractions".equals(key)
                || "flagUrl".equals(key)
                || "moreInfoRequest".equals(key)
                || "moreInfoLabel".equals(key)
                || "message".equals(key);
    }

    private Label createSectionLabel(String title) {
        Label sectionLabel = new Label(title);
        sectionLabel.getStyleClass().add("result-section-title");
        return sectionLabel;
    }

    private String buildTitle(JSONObject json) {
        String name = json.optString("name", "").trim();
        if (name.isBlank()) {
            return "";
        }

        String type = json.optString("type", "").trim();
        if (type.isBlank()) {
            return name;
        }
        return name + " (" + type + ")";
    }

    private String formatKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof JSONArray array) {
            return array.toString();
        }
        if (value instanceof JSONObject object) {
            return object.toString();
        }
        return String.valueOf(value);
    }

    private void clearSupplementaryUi() {
        flagPreview.setImage(null);
        flagPreview.setVisible(false);
        flagPreview.setManaged(false);
        titleLabel.setText("");
        titleLabel.setVisible(false);
        titleLabel.setManaged(false);
        moreInfoButton.setVisible(false);
        moreInfoButton.setManaged(false);
        moreInfoButton.setDisable(true);
        moreInfoButton.setText("Them thong tin");

        cachedMoreInfo = null;
        isLoadingMoreInfo = false;

        cachedHotels = null;
        isLoadingHotels = false;
    }

    private void showMessage(String message) {
        resultContainer.getChildren().clear();

        Label messageLabel = new Label(message == null ? "" : message.trim());
        messageLabel.setWrapText(true);
        messageLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.getStyleClass().add("message-label-style");
        VBox.setVgrow(messageLabel, Priority.ALWAYS);
        resultContainer.getChildren().add(messageLabel);
    }

    private void appendMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Label messageLabel = new Label(message.trim());
        messageLabel.setWrapText(true);
        messageLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.getStyleClass().add("message-label-style");
        VBox.setVgrow(messageLabel, Priority.ALWAYS);
        resultContainer.getChildren().add(messageLabel);
    }

    private static class SearchThreadFactory implements ThreadFactory {
        private final AtomicLong sequence = new AtomicLong(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "SearchThread-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
