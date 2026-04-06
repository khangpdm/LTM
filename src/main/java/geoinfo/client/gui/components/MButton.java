package geoinfo.client.gui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

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
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.out.println("Icon not found: " + path);
            return new ImageView();
        }

        Image image = new Image(is);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);
        return imageView;
    }
}