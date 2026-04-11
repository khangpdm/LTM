package geoinfo.client.gui.components;

import geoinfo.client.network.ClientService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class SearchResultPane extends VBox {
    private static final String META_PREFIX = "__META__";
    private static final ExecutorService SEARCH_EXECUTOR =
            Executors.newCachedThreadPool(new SearchThreadFactory());

    private final AtomicLong latestRequestId = new AtomicLong();
    private final ClientService clientService;
    private final TextArea resultArea;
    private final ImageView flagPreview;
    private final Button moreInfoButton;

    private String pendingMoreInfoRequest;
    private String loadedMoreInfoRequest;

    public SearchResultPane(ClientService clientService) {
        this.clientService = clientService;

        flagPreview = new ImageView();
        flagPreview.setFitHeight(48);
        flagPreview.setFitWidth(72);
        flagPreview.setPreserveRatio(true);
        flagPreview.setVisible(false);
        flagPreview.setManaged(false);

        moreInfoButton = new Button("Them thong tin");
        moreInfoButton.setVisible(false);
        moreInfoButton.setManaged(false);
        moreInfoButton.setDisable(true);
        moreInfoButton.setOnAction(event -> fetchMoreInfo());

        HBox infoBar = new HBox(12, flagPreview, moreInfoButton);
        infoBar.setAlignment(Pos.CENTER_LEFT);

        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setStyle(
                "-fx-background-color: -fx-control-inner-background; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-background-radius: 0;"
        );
        VBox.setVgrow(resultArea, Priority.ALWAYS);

        setSpacing(12);
        getChildren().addAll(infoBar, resultArea);
    }

    public void setText(String text) {
        clearSupplementaryUi();
        resultArea.setText(text);
    }

    public void clear() {
        clearSupplementaryUi();
        resultArea.clear();
    }

    public void search(String request, String loadingMessage) {
        long requestId = latestRequestId.incrementAndGet();
        pendingMoreInfoRequest = null;
        loadedMoreInfoRequest = null;
        clearSupplementaryUi();
        resultArea.setText(loadingMessage);

        SEARCH_EXECUTOR.submit(() -> {
            String response;
            try {
                response = clientService.sendRequest(request);
            } catch (Exception ex) {
                response = "Loi khi tim kiem: " + ex.getMessage();
            }

            ParsedServerResponse parsed = parseServerResponse(response);
            Platform.runLater(() -> {
                if (requestId != latestRequestId.get()) {
                    return;
                }
                applyParsedResponse(parsed);
            });
        });
    }

    private void fetchMoreInfo() {
        String request = pendingMoreInfoRequest;
        if (request == null || request.isBlank()) {
            return;
        }
        if (request.equals(loadedMoreInfoRequest)) {
            return;
        }

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

            ParsedServerResponse parsed = parseServerResponse(response);
            Platform.runLater(() -> {
                if (requestId != latestRequestId.get()) {
                    return;
                }

                resultArea.appendText("\n\n" + parsed.body());
                loadedMoreInfoRequest = request;
                moreInfoButton.setText("Da them thong tin");
                moreInfoButton.setDisable(true);
            });
        });
    }

    private void applyParsedResponse(ParsedServerResponse parsed) {
        resultArea.setText(parsed.body());

        String flagUrl = parsed.meta().getOrDefault("flagUrl", "");
        if (flagUrl.isBlank()) {
            flagPreview.setImage(null);
            flagPreview.setVisible(false);
            flagPreview.setManaged(false);
        } else {
            flagPreview.setImage(new Image(flagUrl, true));
            flagPreview.setVisible(true);
            flagPreview.setManaged(true);
        }

        pendingMoreInfoRequest = parsed.meta().get("moreInfoRequest");
        loadedMoreInfoRequest = null;

        if (pendingMoreInfoRequest == null || pendingMoreInfoRequest.isBlank()) {
            moreInfoButton.setVisible(false);
            moreInfoButton.setManaged(false);
            moreInfoButton.setDisable(true);
            moreInfoButton.setText("Them thong tin");
        } else {
            moreInfoButton.setText(parsed.meta().getOrDefault("moreInfoLabel", "Them thong tin"));
            moreInfoButton.setDisable(false);
            moreInfoButton.setVisible(true);
            moreInfoButton.setManaged(true);
        }
    }

    private void clearSupplementaryUi() {
        flagPreview.setImage(null);
        flagPreview.setVisible(false);
        flagPreview.setManaged(false);
        moreInfoButton.setVisible(false);
        moreInfoButton.setManaged(false);
        moreInfoButton.setDisable(true);
        moreInfoButton.setText("Them thong tin");
    }

    private ParsedServerResponse parseServerResponse(String response) {
        String normalized = response == null ? "" : response.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        Map<String, String> meta = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder();
        boolean readingMeta = true;

        for (String line : lines) {
            if (readingMeta && line.startsWith(META_PREFIX)) {
                int separatorIndex = line.indexOf('=');
                if (separatorIndex > META_PREFIX.length()) {
                    String key = line.substring(META_PREFIX.length(), separatorIndex);
                    String value = line.substring(separatorIndex + 1);
                    meta.put(key, value);
                }
                continue;
            }

            if (readingMeta && line.isEmpty() && !meta.isEmpty()) {
                readingMeta = false;
                continue;
            }

            readingMeta = false;
            body.append(line).append("\n");
        }

        String bodyText = body.toString().stripTrailing();
        return new ParsedServerResponse(meta, bodyText.isBlank() ? normalized.stripTrailing() : bodyText);
    }

    private record ParsedServerResponse(Map<String, String> meta, String body) {
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
