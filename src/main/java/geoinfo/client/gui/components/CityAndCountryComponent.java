package geoinfo.client.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CityAndCountryComponent extends VBox {
    public record Item(String title, String value) { }
    private final GridPane grid = new GridPane();

    public CityAndCountryComponent() {
        getStyleClass().add("cc-container");
        setFillWidth(true);

        grid.getStyleClass().add("cc-grid");
        grid.setHgap(70);
        grid.setVgap(25);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setHgrow(Priority.ALWAYS);
        c2.setFillWidth(true);

        grid.getColumnConstraints().setAll(c1, c2);
        getChildren().add(grid);
    }

    public void setItems(List<Item> items) {
        grid.getChildren().clear();
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Node> cards = new ArrayList<>(items.size());
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            cards.add(createCard(item.title(), item.value()));
        }

        for (int i = 0; i < cards.size(); i++) {
            int row = i / 2;
            int col = i % 2;
            Node card = cards.get(i);
            grid.add(card, col, row);
            GridPane.setHgrow(card, Priority.ALWAYS);

            if (i == cards.size() - 1 && cards.size() % 2 != 0) {
                GridPane.setColumnSpan(card, 2);
            }
        }

        requestLayout();
    }

    private Node createCard(String title, String value) {
        Label titleLabel = new Label(safeText(title));
        titleLabel.getStyleClass().add("cc-card-title");
        titleLabel.setWrapText(true);

        Label valueLabel = new Label(safeText(value));
        valueLabel.getStyleClass().add("cc-card-value");
        valueLabel.setWrapText(true);
        Node valueNode = valueLabel;

        VBox card = new VBox(6, titleLabel, valueNode);
        card.getStyleClass().add("cc-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
