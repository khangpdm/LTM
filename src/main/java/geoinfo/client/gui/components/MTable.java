package geoinfo.client.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class MTable extends VBox {

    public record RowData(String field, String value) {
    }

    public MTable() {
        setSpacing(0);
        setFillWidth(true);
        setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #dbe7f3;" +
                "-fx-background-radius: 10;"
        );
    }

    public void setRows(List<RowData> rows) {
        getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (int index = 0; index < rows.size(); index++) {
            getChildren().add(createRow(rows.get(index), index == rows.size() - 1));
        }
    }

    private HBox createRow(RowData row, boolean isLastRow) {
        Label fieldLabel = new Label(safeText(row.field()));
        fieldLabel.setWrapText(true);
        fieldLabel.setMaxWidth(Double.MAX_VALUE);
        fieldLabel.setStyle(
                "-fx-text-fill: #00AEEF;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 13px;"
        );

        Label valueLabel = new Label(safeText(row.value()));
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setStyle(
                "-fx-text-fill: #6b7280;" +
                "-fx-font-size: 13px;"
        );

        HBox rowBox = new HBox(18, fieldLabel, valueLabel);
        rowBox.setAlignment(Pos.TOP_LEFT);
        rowBox.setPadding(new Insets(12, 16, 12, 16));
        rowBox.setFillHeight(true);
        rowBox.setStyle(
                "-fx-background-color: white;" +
                (isLastRow ? "" : "-fx-border-color: transparent transparent #e5e7eb transparent;")
        );

        fieldLabel.setPrefWidth(220);
        fieldLabel.setMinWidth(220);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        return rowBox;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
