package geoinfo.client.gui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MButton extends Button {

    public MButton(String text, String iconPath) {
        this(text, iconPath, 18, 18);
    }

    public MButton(String text, String iconPath, int iconWidth, int iconHeight) {
        super(text);

        this.getStyleClass().add("m-button");
        this.setAlignment(Pos.CENTER_LEFT);

        if (iconPath != null && !iconPath.isEmpty()) {
            ImageView icon = createIcon(iconPath, iconWidth, iconHeight);
            this.setGraphic(icon);
            this.setContentDisplay(ContentDisplay.LEFT);
            this.setGraphicTextGap(10);
        }
    }

    private ImageView createIcon(String path, int width, int height) {
        Image image = new Image(getClass().getResourceAsStream(path));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        return imageView;
    }
}